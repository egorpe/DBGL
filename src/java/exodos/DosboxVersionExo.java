package exodos;

public class DosboxVersionExo {

	String title_;
	String version_;
	String executable_;
	String conf_;
	boolean multiconf_;
	
	public DosboxVersionExo(String title, String version, String executable, String conf, boolean multiconf) {
		title_ = title;
		version_ = version;
		executable_ = executable;
		conf_ = conf;
		multiconf_ = multiconf;
	}
}
