package beastfx.app.methodsection.objecteditor;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Input;
import beastfx.app.inputeditor.BeautiDoc;

/**
 * HTML version of beast.app.inputeditor.InputEditor.
 * 
 * Base class for editors that provide a GUI for manipulating an Input for a BEASTObject.
 * The idea is that for every type of Input there will be a dedicated editor, e.g.
 * for a String Input, there will be an edit field, for a Boolean Input, there will
 * be a checkbox in the editor.
 * <p/>
 * The default just provides an edit field and uses toString() on Input to get its value.
 * To change the behaviour, override
 * public void init(Input<?> input, BEASTObject beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons)
 */
public interface ObjectEditor {
	

	/** type of object that can be edited with this editor **/
	Class<?> getType();

    /** list of types of BEASTObjects to which this editor can be used **/ 
	default public Class<?>[] getTypes() {
        Class<?>[] types = new Class<?>[1];
        types[0] = getType();
        return types;
    }

	/** generate HTML for editing an object **/
	public String toHTML(Object o, Input<?> input) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException ;
	
	
	
	public abstract class Base implements ObjectEditor {
	    /**
	     * document that we are editing *
	     */
	    protected BeautiDoc doc;
	    
		public Base(BeautiDoc doc) {
			this.doc = doc;
		}

	}

}
