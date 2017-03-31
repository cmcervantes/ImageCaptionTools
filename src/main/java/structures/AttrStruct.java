package structures;

import utilities.StringUtil;

import java.util.*;

/**An implementation of a feature structure used for
 * Annotation attributes, the AttrStruct keeps
 * an internal mapping of basic attributes (k,v pairs)
 * and of nested attrbibutes, which are themselves
 * AttrStructs. The core of each AttrStruct is an
 * annotation (Token / Chunk / Mention), representing
 * the text to which these attributes apply
 *
 */
public class AttrStruct
{
    private Map<String, Set<String>> _attrs;
    private Map<String, Set<AttrStruct>> _nestedAttrs;
    private Annotation _core;

    /**Default constructor, creating an attribute struct
     * around a core Annotation
     */
    public AttrStruct(Annotation core)
    {
        _core = core;
        _attrs = new HashMap<>();
        _nestedAttrs = new HashMap<>();
    }

    /**Deeply copies the given attrStruct
     *
     * @param attrStruct
     */
    public AttrStruct(AttrStruct attrStruct)
    {
        _core = attrStruct._core;
        _attrs = new HashMap<>();
        for(String name : attrStruct._attrs.keySet())
            _attrs.put(name, new HashSet<>(attrStruct._attrs.get(name)));
        _nestedAttrs = new HashMap<>();
        for(String name : attrStruct._nestedAttrs.keySet()){
            Set<AttrStruct> structSet = new HashSet<>();
            attrStruct._nestedAttrs.get(name).stream().forEachOrdered(as -> structSet.add(new AttrStruct(as)));
            _nestedAttrs.put(name, structSet);
        }
    }

    /**Adds a basic (name, value) attribute to this struct
     *
     * @param name
     * @param value
     */
    public void addAttribute(String name, String value)
    {
        if(!_attrs.containsKey(name))
            _attrs.put(name, new HashSet<>());
        _attrs.get(name).add(value);
    }

    /**Adds a nested attribute to this struct, merging if this
     * value's core is present in nested
     *
     * @param name
     * @param value
     */
    public void addAttribute(String name, AttrStruct value)
    {
        if(!_nestedAttrs.containsKey(name))
            _nestedAttrs.put(name, new HashSet<>());
        boolean foundCore = false;
        for(AttrStruct as : _nestedAttrs.get(name)){
            if(as._core.equals(value._core)){
                foundCore = true;
                as.mergeAttr(value);
            }
        }
        if(!foundCore)
            _nestedAttrs.get(name).add(value);
    }

    /**Removes the attribute with name from dict
     *
     * TODO: merge with clearNestedAttribute?
     *
     * @param name
     */
    public void clearAttribute(String name)
    {
        _attrs.remove(name);
    }

    /**Removes the nested attr struct collection
     * with name from dict
     *
     * TODO: merge with clearAttribute?
     *
     * @param name
     */
    public void clearNestedAttribute(String name)
    {
        _nestedAttrs.remove(name);
    }

    /**Returns the AttrStruct attributes beneath the
     * given name; null if this does not contain name
     *
     * @param name
     * @return
     */
    public Collection<AttrStruct> getAttribute_struct(String name)
    {
        return _nestedAttrs.get(name);
    }

    /**Returns the string attributes beneath the
     * given name; null if this does not contain name
     *
     * @param name
     * @return
     */
    public Collection<String> getAttribute_string(String name)
    {
        return _attrs.get(name);
    }

    /**Recursively merges this with the given attrStruct
     *
     * @param attrStruct
     * @return
     */
    private void mergeAttr(AttrStruct attrStruct)
    {
        for(String name : _attrs.keySet())
            _attrs.get(name).addAll(attrStruct._attrs.get(name));

        for(String name : _nestedAttrs.keySet()) {
            if (attrStruct._nestedAttrs.containsKey(name)) {
                Set<AttrStruct> thisVal = _nestedAttrs.get(name);
                Set<AttrStruct> thatVal = attrStruct._nestedAttrs.get(name);
                Set<AttrStruct> remSet = new HashSet<>();
                for (AttrStruct thisAS : thisVal){
                    for (AttrStruct thatAS : thatVal){
                        if (thisAS._core.equals(thatAS._core)) {
                            remSet.add(thatAS);
                            thisAS.mergeAttr(thatAS);
                        }
                    }
                }
                for(AttrStruct thatAS : thatVal)
                    if(!remSet.contains(thatAS))
                        _nestedAttrs.get(name).add(thatAS);
            }
        }
        for(String name : attrStruct._nestedAttrs.keySet())
            if(!_nestedAttrs.containsKey(name))
                _nestedAttrs.put(name, attrStruct._nestedAttrs.get(name));
    }

    /**Returns the total number of attributes in this
     * struct and its nested structs
     *
     * @return
     */
    public int getNumAttributes()
    {
        int numAttr = 0;
        for(String name : _attrs.keySet())
            numAttr += _attrs.get(name).size();
        for(String name : _nestedAttrs.keySet())
            for(AttrStruct as : _nestedAttrs.get(name))
                numAttr += as.getNumAttributes();
        return numAttr;
    }

    /**Returns the number of attributes shared by both
     * this and the given attribute struct, where
     * an attribute is only shared if its in the exact
     * same position in the attribute struct with the same
     * value; note: cores do not have to be equal,
     * as in a union operation
     *
     * @param attrStruct
     * @return
     */
    public int getNumAttributes(AttrStruct attrStruct)
    {
        int numAttr = 0;
        for(String name : _attrs.keySet()){
            Set<String> intersection = new HashSet<>(_attrs.get(name));
            Set<String> thatVal = attrStruct._attrs.get(name);
            if(thatVal != null)
                intersection.retainAll(thatVal);
            numAttr += intersection.size();
        }
        for(String name : _nestedAttrs.keySet()){
            if(attrStruct._nestedAttrs.containsKey(name)){
                numAttr += getAttributeOverlap(this, attrStruct, name);
            }
        }
        return numAttr;
    }

    /**Returns a collection of all attributes and their values in this struct;
     * Flag nested IDs specifies whether to use unique nested IDs
     *      ie. grandparentAttrName>parentAttrName>childAttrName:val
     * Or direct attribute names
     *      ie. attrName:val
     * for nested attributes
     *
     * @param nestedIDs
     * @return
     */
    public Collection<String> getAttributeStrings(boolean nestedIDs)
    {
        List<String> attrStrList = new ArrayList<>();
        for(String name : _attrs.keySet()){
            for(String val : _attrs.get(name))
                attrStrList.add(name + ":" + val);
        }
        for(String name : _nestedAttrs.keySet()){
            for(AttrStruct as : _nestedAttrs.get(name)){
                Collection<String> nestedAttrStr = as.getAttributeStrings(nestedIDs);
                for(String attrStr : nestedAttrStr){
                    String s = attrStr;
                    if(nestedIDs)
                        s = name + ">" + s;
                    attrStrList.add(s);
                }
            }
        }
        return attrStrList;
    }

    /**Returns this attribute structure as a
     * Latex AVM
     *
     * @return
     */
    public String toLatexString()
    {
        //get the attribute lists in order, so we have consistent
        //output
        List<String> baseNames = new ArrayList<>(_attrs.keySet());
        Collections.sort(baseNames);
        List<String> nestedNames = new ArrayList<>(_nestedAttrs.keySet());
        Collections.sort(nestedNames);

        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{avm}[{}\n");
        for(String name : baseNames){
            sb.append(name.replace("_", "\\_"));
            sb.append(": ");

            Set<String> vals = _attrs.get(name);
            if(vals.size() > 1)
                sb.append("\\{");
            sb.append(StringUtil.listToString(_attrs.get(name), ","));
            if(vals.size() > 1)
                sb.append("\\}");
            sb.append("\\cr\n");
        }
        for(String name : nestedNames){
            sb.append(name.replace("_", "\\_"));
            sb.append(": ");

            Set<AttrStruct> vals = _nestedAttrs.get(name);
            if(vals.size() > 1)
                sb.append("\\{");
            for(AttrStruct as : vals){
                sb.append(as.toLatexString());
                sb.append("\\cr\n");
            }
            if(vals.size() > 1)
                sb.append("\\}");
            sb.append("\\cr\n");
        }
        sb.append("]\\end{avm}");
        return sb.toString();
    }

    /**Returns the Mention attributes of this structure
     *
     * @return
     */
    public Collection<Mention> getAttributeMentions()
    {
        Set<Mention> mentionSet = new HashSet<>();
        for(String name : _nestedAttrs.keySet()){
            for(AttrStruct as : _nestedAttrs.get(name)){
                if(as._core instanceof Mention){
                    Mention m = (Mention)as._core;
                    mentionSet.add(m);
                    mentionSet.addAll(as.getAttributeMentions());
                }
            }
        }
        return mentionSet;
    }

    /**Returns whether the given attribute structures have conflicting attribute, where
     * attributes are said to be comparable if their singleton attributes at anchorName
     * are equal, and are said to be in conflict if their other singleton attributes have
     * different values
     *
     * @param attr1
     * @param attr2
     * @param anchorName
     * @return
     */
    public static Boolean hasConflictingAttr(AttrStruct attr1, AttrStruct attr2, String anchorName)
    {
        Set<String> anchor1 = attr1._attrs.get(anchorName);
        Set<String> anchor2 = attr2._attrs.get(anchorName);

        //if these elements didn't have the specified anchors, there's no conflict here
        if(anchor1 != null && anchor2 != null && !anchor1.isEmpty() && !anchor2.isEmpty()){
            //if the anchors aren't single elements, return null
            if(anchor1.size() > 1 || anchor2.size() > 1)
                return null;

            //Otherwise, our anchors are single elements, and we can detect conflicts
            String anchorVal1 = (String)anchor1.toArray()[0];
            String anchorVal2 = (String)anchor1.toArray()[0];

            //if these two anchors aren't the same, there's no conflict
            if(anchorVal1.equals(anchorVal2)){
                //Reaching here means we have single anchor elements that have the same anchor
                //(think: "a blue hat" and "two red hats"); now go through and compare
                //their singleton attributes
                Set<String> attrNames = new HashSet<>(attr1._attrs.keySet());
                attrNames.addAll(attr2._attrs.keySet());
                for(String name : attrNames){
                    if(attr1._attrs.containsKey(name) && attr2._attrs.containsKey(name) &&
                            attr1._attrs.get(name).size() == 1 && attr2._attrs.get(name).size() == 1){
                        String attrVal1 = (String)attr1._attrs.get(name).toArray()[0];
                        String attrVal2 = (String)attr2._attrs.get(name).toArray()[0];
                        if(!attrVal1.equals(attrVal2))
                            return true;
                    }
                }

                //Reaching here mans none of our attributes have conflicting values;
                //now we need to recursively check the nested attributes; we could do this
                //intelligently at this phase - checking for the anchors - but the recursion
                //will take care of that for us, since different-anchor attributes don't conflict
                attrNames = new HashSet<>(attr1._nestedAttrs.keySet());
                attrNames.addAll(attr2._nestedAttrs.keySet());
                for(String name : attrNames){
                    if(attr1._nestedAttrs.containsKey(name) && attr2._nestedAttrs.containsKey(name)){
                        Set<AttrStruct> nestedAttrs1 = attr1._nestedAttrs.get(name);
                        Set<AttrStruct> nestedAttrs2 = attr2._nestedAttrs.get(name);
                        for(AttrStruct nestedAttr1 : nestedAttrs1){
                            for(AttrStruct nestedAttr2 : nestedAttrs2){
                                Boolean nestedConflict =
                                        hasConflictingAttr(nestedAttr1, nestedAttr2, anchorName);
                                if(nestedConflict == null)
                                    return null;
                                else if(nestedConflict)
                                    return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**Returns the number of attributes shared between the two attribute
     * dicts, given the attribute name; given that each may have a collection
     * under the given name, the best match is found
     *
     * @param attr1
     * @param attr2
     * @param attrName
     * @return
     */
    public static int getAttributeOverlap(AttrStruct attr1, AttrStruct attr2, String attrName)
    {
        int numAttr = 0;
        if(attr1._nestedAttrs.containsKey(attrName) && attr2._nestedAttrs.containsKey(attrName)){
            Set<AttrStruct> attrSet1 = new HashSet<>(attr1._nestedAttrs.get(attrName));
            Set<AttrStruct> attrSet2 = new HashSet<>(attr2._nestedAttrs.get(attrName));

            //if both elements have a set of nested attributes under
            //this name, find the attribute in that which best matches this
            while(!attrSet1.isEmpty() && !attrSet2.isEmpty()){
                //find the best this/that pair
                AttrStruct best1 = null;
                AttrStruct best2 = null;
                int max = 0;
                for(AttrStruct val1 : attrSet1){
                    for(AttrStruct val2 : attrSet2){
                        int shareCount = val1.getNumAttributes(val2);
                        if(shareCount > max){
                            best1 = val1;
                            best2 = val2;
                            max = shareCount;
                        }
                    }
                }

                //if we found a best pair, add their counts to the total
                //and remove them from their lists; if we found no
                //best pair, quit
                if(best1 != null && best2 != null){
                    numAttr += max;
                    attrSet1.remove(best1);
                    attrSet2.remove(best2);
                } else {
                    attrSet1 = new HashSet<>();
                }
            }
        }
        return numAttr;
    }
}
