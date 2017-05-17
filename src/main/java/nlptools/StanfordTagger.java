package nlptools;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import structures.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


/**Acceptable Annotators:
 * tokenize     tokenizer
 * ssplit       sentence splitter
 * pos          pos tagger
 * lemma        lemmatizer
 * parse        basic parser
 * depparse     dependency parser
 *
 * Full list in
 * https://stanfordnlp.github.io/CoreNLP/annotators.html
 *
 */

public class StanfordTagger
{

    public StanfordTagger()
    {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // read some text in the text variable
        String text = "Several men and women ride surfboards and jetskis in the ocean waves.";

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);


        //Convert their sentences to our captions
        List<Token> tokens = new ArrayList<>();
        int idx = 0;
        for(CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)){
            List<CoreLabel> tokens_stanford = sentence.get(CoreAnnotations.TokensAnnotation.class);
            //LabeledChunkIdentifier lci = new LabeledChunkIdentifier();
            //List<CoreMap> chunks = lci.getAnnotatedChunks(tokens_stanford, 0, null, null);



            for(CoreLabel tok : sentence.get(CoreAnnotations.TokensAnnotation.class)){
                String word = tok.get(CoreAnnotations.TextAnnotation.class);
                String pos = tok.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lemma = tok.get(CoreAnnotations.LemmaAnnotation.class);
                tokens.add(new Token("none", 0, idx, word, lemma, pos));
                idx++;
            }
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);


            for(Tree t : tree.subTreeList()){
                Label l = t.label();
                String v = t.value();
                System.out.println(l.value() + "|" + v);
            }
            System.exit(0);

            tree.pennPrint();
            for(Constituent phrase : tree.constituents()){
                boolean foundInnerChunk = false;
                for(Constituent innerPhrase : tree.constituents())
                    if(!phrase.equals(innerPhrase) && phrase.contains(innerPhrase))
                        foundInnerChunk = true;
                if(!foundInnerChunk){

                    StringBuilder sb = new StringBuilder();
                    sb.append(" ");
                    for(int i=phrase.start(); i<=phrase.end(); i++){
                        sb.append(tokens.get(i));
                        sb.append(" ");
                    }
                    System.out.println(sb.toString());
                }
            }

            System.exit(0);

            List<Tree> trees = new ArrayList<>();
            trees.addAll(Arrays.asList(tree.children()));
            while(!trees.isEmpty()){
                Tree t = trees.remove(0);
                if(t.children().length == 0)
                    System.out.println(t.toString());
                else
                    trees.addAll(Arrays.asList(tree.children()));
            }
        }

        System.exit(0);



        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for(CoreMap sentence: sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
            }

            // this is the parse tree of the current sentence
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);

            // this is the Stanford dependency graph of the current sentence
            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            dependencies.prettyPrint();
        }

        System.out.println("");
    }
}
