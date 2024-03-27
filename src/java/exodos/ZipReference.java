package exodos;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class ZipReference {

	final ZipFile zipFile_;
	final ZipEntry zipEntry_;
	final String name_;

	ZipReference(ZipFile zipFile, ZipEntry zipEntry, String name) {
		zipFile_ = zipFile;
		zipEntry_ = zipEntry;
		name_ = name;
	}
}