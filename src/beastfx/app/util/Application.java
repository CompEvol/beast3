package beastfx.app.util;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;





@Description("JavaFX application that handles argument parsing by introspection " +
		"using Inputs declared in the class.")
abstract public class Application extends javafx.application.Application {

	/** default input used for argument parsing **/ 
	protected Input<?> defaultInput = null;
	
	/** 
	 * Arguments of the form -name value are processed by finding Inputs
	 * with matching name and setting their value.
	 * 
	 * If the input is a boolean that needs to be set to true, the 'value' a
	 * rgument can be omitted.
	 * 
	 * The last argument is assigned to the defaultInput.
	 * **/
	protected void parseArgs(String [] args, boolean sloppy) throws Exception {
		List<Input<?>> inputs = listInputs();
		for (Input<?> input : inputs) {
			input.determineClass(this);
		}
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			boolean done = false;
			if (arg.startsWith("-")) {
				String name = arg.substring(1);
				String value = (i < args.length - 1 ? args[i+1] : null);
				for (Input<?> input : inputs) {
					if (input.getName().equals(name)) {
						try {
							if (input.getType() == Boolean.class){
								if (value != null && (value.toLowerCase().equals("true") || value.toLowerCase().equals("false"))) {
							        input.setValue(value, null);							
									i++;
								} else {
							        input.setValue(Boolean.TRUE, null);							
								}
							} else {
								input.setValue(value, null);
								i++;
							}
						} catch (Exception e) {
							throw new Exception("Problem parsing arguments:\n" + e.getMessage());
						}
				        done = true;
						break;
					}
				}
			} else {
				if (defaultInput != null) {
					if (defaultInput.getType().isAssignableFrom(List.class)) {
						for (int j = i; j < args.length; j++) {
							arg = args[j];
							if (arg.startsWith("-")) {
								throw new Exception("Problem parsing arguments: are all arguments specified by a dash?");
							}
							defaultInput.setValue(arg, null);
						}
						i = args.length;
						done = true;
					} else if (i == args.length-1) {
						defaultInput.setValue(arg, null);
						done = true;
					}
				}
			}
			if (!done) {
				if (sloppy) {
					Log.info.println("Unknown argument: " + args[i] + " ignored.");
					i++;
				} else {
					throw new Exception("Unknown argument: " + args[i] + "\n" + getUsage());
				}
			}
		}
		
	}
	
	protected void parseArgs(JSONObject args) throws Exception {
		List<String> argList = new ArrayList<String>();
		for (String key : args.keySet()) {
			argList.add("-" + key.trim());
			argList.add(args.get(key).toString().trim());
		}		
		parseArgs(argList.toArray(new String []{}), true);
	}
	
    protected String getUsage() {
    	StringBuffer buf = new StringBuffer();
    	try {
	    	List<Input<?>> inputs = listInputs();
	    	buf.append("Usage: " + getClass().getName() + "\n");
	    	for (Input<?> input : inputs) {
	    		buf.append("-" + input.getName() + " ");
	    		buf.append(input.getValueTipText());
	    		buf.append("\t" + input.getTipText() + "\n");
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
		return buf.toString();
	}

	/**
     * create list of inputs to this Application *
     */
    private List<Input<?>> listInputs() throws IllegalArgumentException, IllegalAccessException {
        List<Input<?>> inputs = new ArrayList<Input<?>>();
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) field.get(this);
                inputs.add(input);
            }
        }
        return inputs;
    } // listInputs

    
    /** class for redirecting log messages to string **/
    public class AppOutputStream extends OutputStream {
        private StringBuilder string = new StringBuilder();
        @Override
        public void write(int b) throws IOException {
            this.string.append((char) b );
        }

        public String toString(){
        	String str = this.string.toString();
        	string.delete(0, string.length());
            return  str;
        }
    };

}
