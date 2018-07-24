package atoml.smoke;

import weka.core.Instances;

/**
 * Interface for the definition of smoke tests
 * 
 * @author sherbold
 *
 */
public interface SmokeTest {

	/**
	 * name of the test
	 * 
	 * @return name
	 */
	String getName();

	/**
	 * get the original data
	 * 
	 * @return original data
	 */
	Instances getData();
	
	/**
	 * get the test data
	 * 
	 * @return test data
	 */
	Instances getTestData();
	
	/**
	 * create new data that is stored internally
	 */
	void createData();
	
	/**
	 * creates new testdata that is stored internally
	 */
	void createTestdata();
}
