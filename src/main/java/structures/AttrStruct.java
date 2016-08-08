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
            attrStruct._nestedAttrs.get(name).forEach(as -> structSet.add(new AttrStruct(as)));
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
                Set<AttrStruct> thisSet = new HashSet<>(_nestedAttrs.get(name));
                Set<AttrStruct> thatSet = new HashSet<>(attrStruct._nestedAttrs.get(name));
                //if both elements have a set of nested attributes under
                //this name, find the attribute in that which best matches this
                while(!thisSet.isEmpty() && !thatSet.isEmpty()){
                    //find the best this/that pair
                    AttrStruct bestThis = null;
                    AttrStruct bestThat = null;
                    int max = 0;
                    for(AttrStruct thisVal : thisSet){
                        for(AttrStruct thatVal : thatSet){
                            int shareCount = thisVal.getNumAttributes(thatVal);
                            if(shareCount > max){
                                bestThis = thisVal;
                                bestThat = thatVal;
                                max = shareCount;
                            }
                        }
                    }

                    //if we found a best pair, add their counts to the total
                    //and remove them from their lists; if we found no
                    //best pair, quit
                    if(bestThis != null && bestThat != null){
                        numAttr += max;
                        thisSet.remove(bestThis);
                        thatSet.remove(bestThat);
                    } else {
                        thisSet = new HashSet<>();
                    }
                }
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
            sb.append("\\\\\n");
        }
        for(String name : nestedNames){
            sb.append(name.replace("_", "\\_"));
            sb.append(": ");

            Set<AttrStruct> vals = _nestedAttrs.get(name);
            if(vals.size() > 1)
                sb.append("\\{");
            for(AttrStruct as : vals){
                sb.append(as.toLatexString());
                sb.append("\\\\\n");
            }
            if(vals.size() > 1)
                sb.append("\\}");
            sb.append("\\\\\n");
        }
        sb.append("]\\end{avm}");
        return sb.toString();
    }
}
