package atoml.testgen;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import atoml.classifiers.Algorithm;
import atoml.classifiers.FeatureType;
import atoml.classifiers.RelationType;
import atoml.data.DataDescription;
import atoml.metamorphic.MetamorphicTest;
import atoml.smoke.SmokeTest;

/**
 * Generates the source code for a test case
 * @author sherbold
 */
public class TestcaseGenerator {
	
	/**
	 * classifier that is tested
	 */
	private final Algorithm algorithmUnderTest;
	
	private final List<DataDescription> morphtestDataDescriptions;
	
	private final int iterations; 
	
	private final boolean useMysql;
	
	private final boolean generateSmokeTests;
	
	private final boolean generateMorphTests;

	private final TemplateEngine templateEngine;
	
	/**
	 * creates a new TestclassGenerator
	 * @param algorithmUnderTest classifier that is tested
	 * @param morphtestDataDescriptions descriptions of the data sets used by morph tests
	 * @param iterations number of iterations for the test (must match with generated data)
	 */
	public TestcaseGenerator(Algorithm algorithmUnderTest, List<DataDescription> morphtestDataDescriptions, int iterations, boolean useMysql, boolean generateSmokeTests, boolean generateMorphTests) {
		this.algorithmUnderTest = algorithmUnderTest;
		this.morphtestDataDescriptions = morphtestDataDescriptions;
		this.iterations = iterations; 
		this.useMysql = useMysql;
		this.generateSmokeTests = generateSmokeTests;
		this.generateMorphTests = generateMorphTests;
		switch(algorithmUnderTest.getFramework()) {
		case "AIToolBox":
			templateEngine = new AIToolBoxTemplate(algorithmUnderTest);
			break;
		case "weka":
			templateEngine = new WekaTemplate(algorithmUnderTest);
			break;
		case "sklearn":
			templateEngine = new SklearnTemplate(algorithmUnderTest);
			break;
		case "spark":
			templateEngine = new SparkTemplate(algorithmUnderTest);
			break;
		case "caret":
			templateEngine = new CaretTemplate(algorithmUnderTest);
			break;
		case "tensorflow":
			templateEngine = new TensorFlowTemplate(algorithmUnderTest);
			break;
		default:
			// TODO
			templateEngine = null;
			System.err.format("Unknown framework: %s%n", algorithmUnderTest.getFramework());
			break;
		}
	}
	
	/**
	 * generates the source code
	 * @return the source code
	 */
	public String generateSource() {
		@SuppressWarnings("resource")
		String classBody = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix()+"-class.template"), "UTF-8").useDelimiter("\\A").next();

		StringBuilder testmethods = new StringBuilder();
		StringBuilder allTests = new StringBuilder();
		
		if( generateMorphTests ) {
			List<MetamorphicTest> metamorphicTests = getMorptests(algorithmUnderTest.getProperties());
			for( MetamorphicTest metamorphicTest : metamorphicTests) {
				for( DataDescription morphtestDataDescription : morphtestDataDescriptions ) {
					// check if  algorithm is compatible with data
					boolean allSupported = true;
					for( FeatureType featureTypeRequired : morphtestDataDescription.getRequiredFeatureTypes() ) {
						boolean isSupported = false;
						for( FeatureType featureTypeSupported : algorithmUnderTest.getFeatures() ) {
							isSupported |= FeatureType.isSupported(featureTypeSupported, featureTypeRequired);
						}
						if( !isSupported ) {
							allSupported = false;
							break;
						}
					}
					// only generate test if both data and test case are compatible
					if( allSupported && metamorphicTest.isCompatibleWithData(morphtestDataDescription) ) {
						testmethods.append(metamorphictestBody(metamorphicTest, morphtestDataDescription));
						allTests.append("        (\"test_" + metamorphicTest.getName() + "_" + morphtestDataDescription.getName() + "\", test_" + metamorphicTest.getName() + "_" + morphtestDataDescription.getName() + "), \n");
					}
				}
			}
		}
		
		if( generateSmokeTests ) {
			List<SmokeTest> smokeTests = getSmoketests(algorithmUnderTest.getFeatures());
			for( SmokeTest smokeTest : smokeTests ) {
				testmethods.append(smoketestBody(smokeTest));
				allTests.append("        (\"test_" + smokeTest.getName() + "\", test_" + smokeTest.getName() + "), \n");
			}
		}
		
		Map<String, String> replacementMap = templateEngine.getClassReplacements();
		replacementMap.put("<<<CLASSNAME>>>", templateEngine.getClassName());
		replacementMap.put("<<<METHODS>>>", testmethods.toString());
		replacementMap.put("<<<ALLTESTS>>>", allTests.toString());
		replacementMap.put("<<<CLASSIFIER>>>", algorithmUnderTest.getClassName());
		
		String mysqlImports = "";
		String mysqlHandler = "";
		String mysqlEvalMorph = "";
		String mysqlEvalSmokeStart = "";
		String mysqlEvalSmokeEnd = "";
		String mysqlIndent = "";
		if( useMysql ) {
			try {
				mysqlImports = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix() + "-mysql-imports.template"), "UTF-8").useDelimiter("\\A").next();
				mysqlHandler = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix() + "-mysql-handler.template"), "UTF-8").useDelimiter("\\A").next();
				mysqlEvalMorph = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix() + "-mysql-morph.template"), "UTF-8").useDelimiter("\\A").next();
				mysqlEvalSmokeStart = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix() + "-mysql-smoke-start.template"), "UTF-8").useDelimiter("\\A").next();
				mysqlEvalSmokeEnd = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix() + "-mysql-smoke-end.template"), "UTF-8").useDelimiter("\\A").next();
				mysqlIndent = "    ";
			} catch (NullPointerException e){
				if (algorithmUnderTest.getFramework().equals("AIToolBox")) {
					System.err.format("WARNING: mysql option not supported for framework %s%n", algorithmUnderTest.getFramework());
				} else {
					throw new RuntimeException("could not access mysql templates: ", e);
				}
			}
		}
		replacementMap.put("<<<MYSQLIMPORTS>>>", mysqlImports);
		replacementMap.put("<<<MYSQLHANDLER>>>", mysqlHandler);
		
		for( String key : replacementMap.keySet()) {
			classBody = classBody.replaceAll(key, replacementMap.get(key));
		}
		// ensures this happens after methods replacement
		classBody = classBody.replaceAll("<<<MYSQLEVALMORPH>>>", mysqlEvalMorph);
		classBody = classBody.replaceAll("<<<MYSQLEVALSMOKE_START>>>", mysqlEvalSmokeStart);
		classBody = classBody.replaceAll("<<<MYSQLEVALSMOKE_END>>>", mysqlEvalSmokeEnd);
		classBody = classBody.replaceAll("<<<MYSQLINDENT>>>", mysqlIndent);
		
		return classBody;
	}
	
	private String smoketestBody(SmokeTest smokeTest) {
		int iterations;
		if( smokeTest.isRandomized() ) {
			iterations = this.iterations;
		} else {
			iterations = 1;
		}

		String smokeResourceEnding = new String();
		if( Boolean.getBoolean("atoml.savepredictions") && this.algorithmUnderTest.getAlgorithmType().equals("classification") ) {
			smokeResourceEnding = "-smoketest-csv.template";
		} else {
			smokeResourceEnding = "-smoketest.template";
		}
		@SuppressWarnings("resource")
		String methodBody = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix()+smokeResourceEnding), "UTF-8").useDelimiter("\\A").next();

		Map<String, String> replacementMap = templateEngine.getSmoketestReplacements(smokeTest);
		replacementMap.put("<<<NAME>>>", smokeTest.getName());
		replacementMap.put("<<<TIMEOUT>>>", System.getProperty("atoml."+this.algorithmUnderTest.getFramework()+".timeout"));
		replacementMap.put("<<<ITERATIONS>>>", Integer.toString(iterations));
		replacementMap.put("<<<IDENTIFIER>>>", this.algorithmUnderTest.getName());
		for( String key : replacementMap.keySet()) {
			methodBody = methodBody.replaceAll(key, replacementMap.get(key));
		}
		
		return methodBody;
	}
	
	private String metamorphictestBody(MetamorphicTest metamorphicTest, DataDescription morphtestDataDescription) {
		int iterations;
		if( morphtestDataDescription.isRandomized() ) {
			iterations = this.iterations;
		} else {
			iterations = 1;
		}
		try {
			@SuppressWarnings("resource")
			String methodBody = new Scanner(this.getClass().getResourceAsStream(getResourcePrefix() + "-morphtest.template"), "UTF-8").useDelimiter("\\A").next();

			Map<String, String> replacementMap = templateEngine.getMorphtestReplacements(metamorphicTest);
			replacementMap.put("<<<NAME>>>", metamorphicTest.getName());
			replacementMap.put("<<<TIMEOUT>>>", System.getProperty("atoml." + this.algorithmUnderTest.getFramework() + ".timeout"));
			replacementMap.put("<<<DATASET>>>", morphtestDataDescription.getName());
			replacementMap.put("<<<CLASSNAME>>>", templateEngine.getClassName());
			replacementMap.put("<<<ITERATIONS>>>", Integer.toString(iterations));
			for (String key : replacementMap.keySet()) {
				methodBody = methodBody.replaceAll(key, replacementMap.get(key));
			}
			return methodBody;
		} catch (NullPointerException e){
			throw new RuntimeException("could not access morphtest.template: ", e);
		}
	}

	
	/**
	 * The path where the test case should be stored. This path is relative to the testsuite source folder.
	 * @return
	 */
	public String getFilePath() {
		return templateEngine.getFilePath();
	}
	
	private String getResourcePrefix() {
		return "/"+algorithmUnderTest.getFramework().toLowerCase()+"-"+algorithmUnderTest.getAlgorithmType();
	}
	
	private List<SmokeTest> getSmoketests(List<FeatureType> features) {
		List<SmokeTest> supportedSmokeTests = new LinkedList<>();
		for( SmokeTest smokeTest : TestCatalog.SMOKETESTS ) {
			for( FeatureType featureType : features ) {
				if(FeatureType.isSupported(featureType, smokeTest.getFeatureType())) {
					supportedSmokeTests.add(smokeTest);
					break;
				}
			}
		}
		return supportedSmokeTests;
	}
	
	private List<MetamorphicTest> getMorptests(Map<String, RelationType> properties) {
		List<MetamorphicTest> supportedMorphTests = new LinkedList<>();
		for( MetamorphicTest morphTest : TestCatalog.METAMORPHICTESTS ) {
			RelationType propertyValue = properties.get(morphTest.getClass().getSimpleName().toUpperCase());
			if( propertyValue!=null ) {
				supportedMorphTests.add(morphTest);
			}
		}
		return supportedMorphTests;
	}
}
