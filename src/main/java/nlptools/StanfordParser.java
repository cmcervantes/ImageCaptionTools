package nlptools;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;
import structures.Caption;
import structures.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**Wrapper for the Stanford Dependency Parser
 * (default model)
 *
 * NOTE: While I want to deprecate this,
 *       there's apparently not an easy way
 *       to get the dependency trees out of the more
 *       up-to-date annotator found in StanfordAnnotator
 *
 * @author ccervantes
 */
public class StanfordParser
{
    edu.stanford.nlp.parser.nndep.DependencyParser _parser;

    /**Constructs a new dependency parser using
     * the default model
     */
    public StanfordParser()
    {
        _parser = edu.stanford.nlp.parser.nndep.DependencyParser.loadFromModelFile(
                edu.stanford.nlp.parser.nndep.DependencyParser.DEFAULT_MODEL);
    }

    /**Parses the given caption with the Stanford Dependency Parser
     * and returns a set of dependency strings in the format
     * gov_token_idx|relation|dep_token_idx
     *
     * @param c
     * @return
     */
    public Collection<String> predict(Caption c)
    {
        //convert our tokens to TaggedWords, for the parser
        List<TaggedWord> wordList = new ArrayList<>();
        for(Token t : c.getTokenList()){
            wordList.add(new TaggedWord(t.toString(), t.getPosTag()));
        }

        //get a grammatical structure corresponding to the dependency prediction
        //and reduce it to a set of dependency strings (govIdx|rel|depIdx)
        GrammaticalStructure gs = _parser.predict(wordList);
        List<String> depStrList = new ArrayList<>();
        for(TypedDependency td : gs.typedDependencies()){
            /* Due to an unfortunate issue with Stanford NLP versioning,
             * I'm commmenting this out and using the strings
             */
            /*
            int tokenIdx_gov = td.gov().index() - 1;
            int tokenIdx_dep = td.dep().index() - 1;
            String rel = td.reln().getShortName();
            */

            String[] tdArr = td.toString().split(", ");
            String dep = tdArr[1].substring(0, tdArr[1].length()-1);
            String[] depArr = dep.split("-");
            dep = depArr[depArr.length-1];
            int idx = tdArr[0].indexOf("(");
            String rel = tdArr[0].substring(0, idx);
            String gov = tdArr[0].substring(idx+1, tdArr[0].length());
            String[] govArr = gov.split("-");
            gov = govArr[govArr.length-1];

            int tokenIdx_gov = Integer.parseInt(gov) - 1;
            int tokenIdx_dep = Integer.parseInt(dep) - 1;

            //token indices are offset by one, since it inserts ROOT as 0
            depStrList.add(tokenIdx_gov + "|" + rel + "|" + tokenIdx_dep);
        }
        return depStrList;
    }
}
