package atoml;

import java.util.List;

import org.apache.commons.cli.ParseException;

import atoml.classifiers.Algorithm;
import atoml.classifiers.YamlClassifierGenerator;
import atoml.testgen.TestsuiteGenerator;

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
		
	    final String yamlFileStr = cmdParameters.getStringValue("file");
	    
		final int iterations = cmdParameters.getIntegerValue("iterations");
	    final int numInstances = cmdParameters.getIntegerValue("instances");
	    final int numFeatures = cmdParameters.getIntegerValue("features");
	    final boolean useMysql = cmdParameters.hasOption("mysql");
	    final boolean generateSmokeTests = !cmdParameters.hasOption("nosmoke");
	    final boolean generateMorphTests = !cmdParameters.hasOption("nomorph");
	    System.setProperty("atoml.savepredictions", String.valueOf(cmdParameters.hasOption("predictions")));

		System.setProperty("atoml.weka.timeout", String.valueOf(1000 * cmdParameters.getIntegerValue("timeout")));
		System.setProperty("atoml.sklearn.timeout", String.valueOf(cmdParameters.getIntegerValue("timeout")));
		System.setProperty("atoml.spark.timeout", String.valueOf(1000 * cmdParameters.getIntegerValue("timeout")));
		System.setProperty("atoml.caret.timeout", String.valueOf(cmdParameters.getIntegerValue("timeout")));
		System.setProperty("atoml.tensorflow.timeout", String.valueOf(cmdParameters.getIntegerValue("timeout")));

		List<Algorithm> algorithms = YamlClassifierGenerator.parseFile(yamlFileStr);
		TestsuiteGenerator testsuiteGenerator = new TestsuiteGenerator(numFeatures, numInstances);
		System.setProperty("atoml.AIToolBox.datapath", "generated-tests/AIToolBox/Tests/AIToolBoxTests/");
		System.setProperty("atoml.AIToolBox.testcasepath", "generated-tests/AIToolBox/Tests/AIToolBoxTests/");
		System.setProperty("atoml.weka.datapath", "generated-tests/weka/src/test/resources/");
		System.setProperty("atoml.weka.testcasepath", "generated-tests/weka/src/test/java/");
		System.setProperty("atoml.sklearn.datapath", "generated-tests/sklearn/");
		System.setProperty("atoml.sklearn.testcasepath", "generated-tests/sklearn/");
		System.setProperty("atoml.spark.datapath", "generated-tests/spark/src/test/resources/");
		System.setProperty("atoml.spark.testcasepath", "generated-tests/spark/src/test/java/");
		System.setProperty("atoml.tensorflow.datapath", "generated-tests/tensorflow/");
		System.setProperty("atoml.tensorflow.testcasepath", "generated-tests/tensorflow/");
		System.setProperty("atoml.caret.datapath", "generated-tests/caret/");
		System.setProperty("atoml.caret.testcasepath", "generated-tests/caret/");
		testsuiteGenerator.generateTests(algorithms, iterations, useMysql, generateSmokeTests, generateMorphTests);
	}
}
