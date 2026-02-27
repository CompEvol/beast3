/*
* File BooleanParameter.java
*
* Copyright (C) 2010 Joseph Heled jheled@gmail.com
*
* This file is part of BEAST.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.base.spec.inference.parameter;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Bool;
import beast.base.spec.type.BoolScalar;
import org.w3c.dom.Node;

import java.io.PrintStream;


/**
 * A scalar boolean-valued parameter in the MCMC state.
 * Implements {@link BoolScalar} for typed access.
 * The domain is fixed to {@link Bool}.
 */
@Description("A Boolean-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class BoolScalarParam extends StateNode implements BoolScalar {

    final public Input<Boolean> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            false, Input.Validate.REQUIRED, Boolean.class);
    /**
     * the actual values of this parameter
     */
    protected boolean value;
    protected boolean storedValue;

    // domain is fixed
//    final private Bool domain = Bool.INSTANCE;

    public BoolScalarParam() {
    }

    public BoolScalarParam(boolean value) {
        // Note sync Input which will assign value in initAndValidate()
        valuesInput.setValue(value, this);

        // always validate
        initAndValidate();
    }

    @Override
    public void initAndValidate() {
        this.value = valuesInput.get();
        this.storedValue = value;

        if (!isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + getDomain().getClass().getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean get() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Bool getDomain() {
        return Bool.INSTANCE;
    }

    //*** setValue ***

    /**
     * Sets the parameter value.
     *
     * @param value the new boolean value
     * @throws IllegalArgumentException if the value fails domain validation
     */
    public void set(boolean value) {
        startEditing(null);

        if (!isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + getDomain().getClass().getName());
        }
        this.value = value;
    }

    // enforce the correct domain
//    public void setDomain(Bool domain) {
//        if (! domain.equals(Bool.INSTANCE))
//            throw new IllegalArgumentException();
//    }

    //*** StateNode methods ***

    @Override
    public void init(PrintStream out) {
        out.print(getID() + "\t");
    }

    @Override
    public void log(long sample, PrintStream out) {
        out.print(get() + "\t");
    }

    @Override
    public void close(PrintStream out) {
        // nothing to do
    }

    @Override
    protected void store() {
        storedValue = value;
    }

    @Override
    public void restore() {
        value = storedValue;
        setEverythingDirty(false);
    }


    @Override
    public void setEverythingDirty(boolean isDirty) {
        setSomethingIsDirty(isDirty);
    }

    @Override
    public StateNode copy() {
        try {
            @SuppressWarnings("unchecked") final BoolScalarParam copy = (BoolScalarParam) this.clone();
            return copy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void assignFromFragile(final StateNode other) {
        @SuppressWarnings("unchecked") final BoolScalarParam source = (BoolScalarParam) other;
        set(source.get());
//        setBounds(source.getLower(), source.getUpper());
    }

    @Override
    public void assignTo(StateNode other) {
        @SuppressWarnings("unchecked") final BoolScalarParam copy = (BoolScalarParam) other;
        copy.setID(getID());
        copy.index = index;
        copy.set(get());
    }

    @Override
    public void assignFrom(final StateNode other) {
        @SuppressWarnings("unchecked") final BoolScalarParam source = (BoolScalarParam) other;
        setID(source.getID());
        set(source.get());
        storedValue = source.storedValue;
    }

    //*** resume ***

    @Override
    public void fromXML(Node node) {
        ParameterUtils.parseParameter(node, this);
    }

    void fromXML(final String valueStr) {
        set(Boolean.parseBoolean(valueStr));
    }

    @Override
    public String toString() {
        return ParameterUtils.paramToString(this);
    }

}
