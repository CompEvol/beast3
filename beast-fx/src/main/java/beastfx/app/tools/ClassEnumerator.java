package beastfx.app.tools;

import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Runnable;
import beast.pkgmgmt.PackageManager;


@Description("Lists classes of a given type. Useful for developers that "
		+ "want to set up an ant configuration file listing services of given type.")
public class ClassEnumerator extends Runnable {

	final public Input<String> typeInput = new Input<>("type", "class for which all implementations will be listed", Validate.REQUIRED); 
	final public Input<String> packageInput = new Input<>("package", "only classes inside this package will be listed", "beast"); 

	final public Input<Boolean> serviceFormatInput = new Input<>("serviceFormat", "list classes in service provider format for ant script", true);
	final public Input<Boolean> moduleeFormatInput = new Input<>("moduleFormat", "list classes in format for module-info.java", false);
	
	@Override
	public void initAndValidate() {
	}

	@Override
	public void run() throws Exception {
		List<String> list = PackageManager.find(typeInput.get(), packageInput.get());
		for (String clazz : list) {
			System.out.println(clazz);
		}

		if (serviceFormatInput.get()) {
			System.out.println("\n\n\n");
			System.out.println("\t<service type=\""+ typeInput.get()+"\">");
			for (String clazz : list) {
				System.out.println("\t\t<provider classname=\"" + clazz + "\"/>");
			}
			System.out.println("\t</service>");
		}

		if (moduleeFormatInput.get()) {
			System.out.println("\n\n\n");
			System.out.println("\tprovides "+ typeInput.get()+" with");
			for (int i = 0; i < list.size(); i++) {
				String clazz = list.get(i);
				System.out.println("\t\t" + clazz + (i < list.size()-1 ? "," : ";"));
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new Application(new ClassEnumerator(), "Class enumerator", args);
	}

}
