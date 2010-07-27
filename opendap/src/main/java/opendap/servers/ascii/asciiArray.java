/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////



package opendap.servers.ascii;

import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.*;

/**
 */
public class asciiArray extends DArray implements toASCII {

    private static boolean _Debug = false;

    /**
     * Constructs a new <code>asciiArray</code>.
     */
    public asciiArray() {
        this(null);
    }

    /**
     * Constructs a new <code>asciiArray</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public asciiArray(String n) {
        super(n);
    }


    /**
     * Returns a string representation of the variables value. This
     * is really foreshadowing functionality for Server types, but
     * as it may come in useful for clients it is added here. Simple
     * types (example: DFloat32) will return a single value. DConstuctor
     * and DVector types will be flattened. DStrings and DURL's will
     * have double quotes around them.
     *
     * @param addName is a flag indicating if the variable name should
     *                appear at the begining of the returned string.
     */
    public void toASCII(PrintWriter pw,
                        boolean addName,
                        String rootName,
                        boolean newLine) {

        if (_Debug) {
            System.out.println("asciiArray.toASCII(" + addName + ",'" + rootName + "')  getName(): " + getName());
            System.out.println("  PrimitiveVector size = " + getPrimitiveVector().getLength());
        }

        rootName = toASCIIAddRootName(pw, addName, rootName);

        if (addName)
            pw.print("\n");

        int dims = numDimensions();
        int shape[] = new int[dims];
        int i = 0;

        for (Enumeration e = getDimensions(); e.hasMoreElements();) {
            DArrayDimension d = (DArrayDimension) e.nextElement();
            shape[i++] = d.getSize();
        }
        int totalCount = asciiArray(pw, addName, "", 0, dims, shape, 0);

        if (newLine)
            pw.print("\n");

    }


    public String toASCIIFlatName(String rootName) {

        String s = "";
        if (rootName != null) {
            s = rootName + "." + getName();
        } else {
            s = getName();
        }


        String s2 = "";
        PrimitiveVector pv = getPrimitiveVector();

        if (pv instanceof BaseTypePrimitiveVector) {

            BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(0);

            if (bt instanceof DString) {

                int dims = numDimensions();
                int i = 0;
                for (Enumeration e = getDimensions(); e.hasMoreElements();) {
                    DArrayDimension d = (DArrayDimension) e.nextElement();
                    s += "[" + d.getSize() + "]";
                }
                s2 = s;
            } else {
                s2 = ((toASCII) bt).toASCIIFlatName(s);
            }

        } else {
            int dims = numDimensions();
            int i = 0;
            for (Enumeration e = getDimensions(); e.hasMoreElements();) {
                DArrayDimension d = (DArrayDimension) e.nextElement();
                s += "[" + d.getSize() + "]";
            }
            s2 = s;
        }
        return (s2);
    }


    public String toASCIIAddRootName(PrintWriter pw, boolean addName, String rootName) {

        if (addName) {
            rootName = toASCIIFlatName(rootName);
            pw.print(rootName);
        }
        return (rootName);

    }


    /**
     * Print an array. This is a private member function.
     *
     * @param os     is the stream used for writing
     * @param index  is the index of VEC to start printing
     * @param dims   is the number of dimensions in the array
     * @param shape  holds the size of the dimensions of the array.
     * @param offset holds the current offset into the shape array.
     * @return the number of elements written.
     */
    private int asciiArray(PrintWriter os, boolean addName, String label, int index, int dims, int shape[],
                           int offset) {

        //os.println("\n\n");
        //os.println("\tdims:   " + dims);
        //os.println("\toffset: " + offset);
        //os.println("\tshape["+offset+"]: " + shape[offset]);
        //os.println("\tindex: " + index);
        //os.println("\n");

        if (dims == 1) {

            if (addName)
                os.print(label);

            for (int i = 0; i < shape[offset]; i++) {


                PrimitiveVector pv = getPrimitiveVector();

                if (pv instanceof BaseTypePrimitiveVector) {

                    BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(index++);

                    if (i > 0) {
                        if (bt instanceof DString)
                            os.print(", ");
                        else
                            os.println("");
                    }
                    ((toASCII) bt).toASCII(os, false, null, false);


                } else {

                    if (i > 0)
                        os.print(", ");
                    pv.printSingleVal(os, index++);
                }


            }
            if (addName) os.print("\n");

            return index;
        } else {
            for (int i = 0; i < shape[offset]; i++) {
                String s = label + "[" + i + "]";
                if ((dims - 1) == 1)
                    s += ", ";
                index = asciiArray(os, addName, s, index, dims - 1, shape, offset + 1);
            }
            return index;

        }
    }


}


