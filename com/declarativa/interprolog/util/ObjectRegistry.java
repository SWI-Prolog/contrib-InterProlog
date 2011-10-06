/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2002
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.util;
import com.declarativa.interprolog.*;
import java.util.Vector;

/** Remembers a set of Java objects, allowing access to them by an (integer) ID */
public class ObjectRegistry{
	Vector objects;
	
	public ObjectRegistry(){
		objects = new Vector();
	}
	
	public Object getRealJavaObject(InvisibleObject o){
		return getRealJavaObject(o.ID);
	}
	
	public Object getRealJavaObject(int ID){
		if (ID<0 | ID>objects.size()-1) 
			throw new RuntimeException("Bad object ID in ObjectRegistry");
		return objects.elementAt(ID);
	}
	
	public Object makeInvisible(Object x){
		return new InvisibleObject(registerJavaObject(x));
	}
	
	public synchronized int registerJavaObject(Object x){
		if (x==null)
			throw new IPException("Null object in ObjectRegistry");
		int i = getObjectID(x);
		if (i>=0) return i;
		objects.addElement(x);
		return objects.size()-1;
	}
	
	private int getObjectID(Object obj){
            // commented next line because there might be null elements in the 
            // registry that are there as a result of unregistering
		//return objects.indexOf(obj); 
            
            if (obj == null) {
                for (int i = 0 ; i < objects.size() ; i++)
                    if (objects.elementAt(i)==null)
                        return i;
            } else {
                for (int i = 0 ; i < objects.size() ; i++){
                    Object elem = objects.elementAt(i);
                    if(elem != null){ // this line is not there in Vector.indexOf
                        if (obj.equals(elem))
                            return i;
                    }
                }
            }
            return -1;
	}
        
        public synchronized boolean unregisterJavaObject(int ID){
            if ((ID < 0) || (ID > objects.size()-1)) {
                return false;
            } else {
                // objects.remove(index) would not work since that operation
                // shifts any subsequent elements to the left (subtracts one from their indices)
                objects.setElementAt(null, ID);
                return true;
            }
        }
        
        public synchronized boolean unregisterJavaObject(Object obj){
            boolean found = false;
            if (obj != null){
                int index = getObjectID(obj);
                if(index >= 0){
                    // objects.remove(index) would not work since that operation
                    // shifts any subsequent elements to the left (subtracts one from their indices)
                    objects.setElementAt(null, index);
                    found = true;
                }
            }
            return found;
        }
        
        public synchronized boolean unregisterJavaObjects(Class cls){
            boolean found = false;
            if (cls != null){
                String className = cls.getName();
                for(int iterator = 0; iterator < objects.size(); iterator++){
                    Object currentObject = objects.elementAt(iterator);
                    if(currentObject != null){
                        if(currentObject.getClass().getName().equals(className)){
                            objects.setElementAt(null, iterator);
                            found = true;
                        }
                    }
                }
                
                // discard null elements in the end of the object registry
                // not sure that it is a good idea - on one hand, if people
                // do not program clean and still refer to the old
                // objects, they will get null pointer exception instead of
                // unpredicatble results. On the other hand, the registry 
                // might become very big and slow too soon. May be this
                // part should go into separate method.
               /* int newSize;
                for(newSize = objects.size(); newSize > 0; newSize--){
                    if(objects.elementAt(newSize - 1) != null) break;
                }
                objects.setSize(newSize); */
            }
            return found;
	}
}
