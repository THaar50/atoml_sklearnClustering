package atoml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;

import atoml.classifiers.Classifier;
import atoml.classifiers.ScikitClassifier;
import atoml.classifiers.WekaClassifier;
import atoml.metamorphic.ConstantChange;
import atoml.metamorphic.DuplicateData;
import atoml.metamorphic.InvertedClass;
import atoml.metamorphic.MetamorphicTest;
import atoml.metamorphic.ReorderAttributes;
import atoml.metamorphic.SameData;
import atoml.metamorphic.ScrambleInstances;
import atoml.smoke.AllZeroes;
import atoml.smoke.LeftSkewed;
import atoml.smoke.NonoverlappingTrainingAndTestData;
import atoml.smoke.Outliers;
import atoml.smoke.RightSkewed;
import atoml.smoke.SingleInstanceMinorityClass;
import atoml.smoke.SmokeTest;
import atoml.smoke.SmoketestFromArff;
import atoml.smoke.SpreadMixture;
import atoml.smoke.UniformDoubleMachinePrecision;
import atoml.smoke.UniformLarge;
import atoml.smoke.UniformSmall;
import atoml.smoke.UniformVeryLarge;
import atoml.smoke.UniformVerySmall;
import atoml.smoke.UniformWholeDoubleRange;
import atoml.smoke.UniformZeroToOne;
import atoml.testgen.ScikitTestsuiteGenerator;
import atoml.testgen.WekaTestsuiteGenerator;

/**
 * Application object that executes the main function.
 * @author sherbold
 */
public class Runner {
	
	/**
	 * Main function
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
	    CmdParameters cmdParameters;
		try {
			cmdParameters = new CmdParameters(args);
		} catch (ParseException e) {
			return;
		}
		
	    final String classifierStr = cmdParameters.getStringValue("classifier");
	    final String inputFileStr = cmdParameters.getStringValue("file");
	    
		final int iterations = cmdParameters.getIntegerValue("iterations");
	    final int numInstances = cmdParameters.getIntegerValue("ninst");
	    final int numFeatures = cmdParameters.getIntegerValue("nfeat");
	    
	    final String testSrcPath = cmdParameters.getStringValue("testpath");
	    final String testResourcePath = cmdParameters.getStringValue("resourcepath");
	    
	    final String mllib = cmdParameters.getStringValue("mllib");
	    
	    List<SmokeTest> smokeTests = new LinkedList<>();
		smokeTests.add(new AllZeroes());
		smokeTests.add(new UniformZeroToOne());
		smokeTests.add(new UniformLarge());
		smokeTests.add(new UniformVeryLarge());
		smokeTests.add(new UniformWholeDoubleRange());
		smokeTests.add(new UniformSmall());
		smokeTests.add(new UniformVerySmall());
		smokeTests.add(new UniformDoubleMachinePrecision());
		smokeTests.add(new SpreadMixture());
		smokeTests.add(new Outliers());
		smokeTests.add(new SingleInstanceMinorityClass());
		smokeTests.add(new NonoverlappingTrainingAndTestData());
		smokeTests.add(new LeftSkewed());
		smokeTests.add(new RightSkewed());
		smokeTests.add(new SmoketestFromArff("CM1", "/smokedata/cm1.arff"));
		List<MetamorphicTest> metamorphicTests = new LinkedList<>();
		metamorphicTests.add(new ConstantChange());
		metamorphicTests.add(new InvertedClass());
		metamorphicTests.add(new ScrambleInstances());
		metamorphicTests.add(new ReorderAttributes());
		metamorphicTests.add(new DuplicateData());
		metamorphicTests.add(new SameData());
	    
		List<Classifier> classifiersUnderTest = new LinkedList<>();
		if( classifierStr!=null ) {
			if( "weka".equals(mllib) ) {
				classifiersUnderTest.add(new WekaClassifier(classifierStr));
			}
			else if( "scikit".equals(mllib) ) {
				classifiersUnderTest.add(new ScikitClassifier(classifierStr));
			} else {
				throw new RuntimeException("invalid option for mllib: " + mllib);
			}
		}
		if( inputFileStr!=null ) {
			try(Stream<String> stream = Files.lines(Paths.get(inputFileStr))) {
				stream.forEach(new Consumer<String>() {
					@Override
					public void accept(String line) {
						if( "weka".equals(mllib) ) {
							classifiersUnderTest.add(new WekaClassifier(line));
						}
						else if( "scikit".equals(mllib) ) {
							classifiersUnderTest.add(new ScikitClassifier(line));
						} else {
							throw new RuntimeException("invalid option for mllib: " + mllib);
						}
					}
				});
			} catch (IOException e) {
				System.out.println("problem reading " + inputFileStr + ": " + e.getMessage());
				return;
			}
		}
		
		if( "weka".equals(mllib) ) {
			WekaTestsuiteGenerator junitGenerator = new WekaTestsuiteGenerator(testSrcPath, testResourcePath, numFeatures, numInstances);
			junitGenerator.generateTests(classifiersUnderTest, smokeTests, metamorphicTests, iterations);
		}
		else if( "scikit".equals(mllib) ) {
			ScikitTestsuiteGenerator scikitGenerator = new ScikitTestsuiteGenerator(testSrcPath, testResourcePath, numFeatures, numInstances);
			scikitGenerator.generateTests(classifiersUnderTest, smokeTests, metamorphicTests, iterations);
		} else {
			throw new RuntimeException("invalid option for mllib: " + mllib);
		}
	}
}
