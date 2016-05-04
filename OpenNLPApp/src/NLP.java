
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import static org.apache.commons.lang3.math.NumberUtils.isParsable;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author aurel
 */
public class NLP {

    public static SentenceDetectorME sentenceDetector;
    public static Tokenizer tokenizer;
    public static ArrayList listeFruits = new ArrayList();
    public static POSModel model;
    public static PerformanceMonitor perfMon;
    public static POSTaggerME tagger;
    public static String[] tags;
    public static String[] tokens;
    public static HashMap<String, String> itemsList;

    public NLP() throws FileNotFoundException, IOException, URISyntaxException {
        itemsList = new HashMap<String, String>();
        
        String file = (new File(NLP.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())).toString();
        String path = (new File(file).getParentFile().getPath()).toString();
        
        model = new POSModelLoader().load(new File(path + "\\fr-pos.bin"));
        perfMon = new PerformanceMonitor(System.err, "sent");
        tagger = new POSTaggerME(model);

        try (InputStream is = new FileInputStream(path + "\\fr-token.bin")) {
            tokenizer = new TokenizerME(new TokenizerModel(is));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String Tokenize(String phrase) throws InvalidFormatException, IOException {
        String[] tags = POSTag(phrase);
        tokens = tokenizer.tokenize(phrase);
        String item = "";
        String price = "";

        for (String token : tokens) {
            if (isParsable(token)) {
                price = token + " €";
                for (int i = 0; i < tokens.length; i++) {
                    if (tags[i].equals("DET") && !tokens[i].equals("de")) {
                        item = tokens[i].toLowerCase() + " ";
                    } else if (tags[i].equals("NC") && !tokens[i].equals("prix")) {
                        item += tokens[i].toLowerCase();
                        itemsList.put(item, price);
                        return "C'est noté : " + item + " coûte " + price;
                    }
                }
            }
        }

        if (tags[0].equals("ADJWH") || tags[0].equals("CS") || tags[0].equals("ADJWH") || tags[0].equals("ADVWH")) {
            for (String key : itemsList.keySet()) {
                if (phrase.contains(key)) {
                    return "Pour " + key + " c'est " + itemsList.get(key);
                }
            }
            return "On ne vend pas ça ici !";
        }

        return "Je ne comprends pas ...";
    }

    public static String[] POSTag(String input) throws IOException {

        ObjectStream<String> lineStream = new PlainTextByLineStream(new StringReader(input));

        return tagger.tag(WhitespaceTokenizer.INSTANCE.tokenize(lineStream.read()));
    }

    /* public static void findName(String[] tokens) throws IOException {
        InputStream is = new FileInputStream("en-ner-person.bin");

        TokenNameFinderModel model = new TokenNameFinderModel(is);
        is.close();

        NameFinderME nameFinder = new NameFinderME(model);

        String[] sentence = tokens;

        Span nameSpans[] = nameFinder.find(sentence);

        System.out.println(nameSpans[0].toString());

    }
    
    public static String SentenceDetect(String paragraph) throws InvalidFormatException, IOException {
        String sentences[] = sentenceDetector.sentDetect(paragraph);
        String output = "";
        for (String sentence : sentences) {
            output += sentence + "\n";
        }
        return output;
    }

    public static void chunk() throws IOException {
        POSModel model = new POSModelLoader().load(new File("en-pos-maxent.bin"));
        PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
        POSTaggerME tagger = new POSTaggerME(model);

        String input = "I give an apple to my mother.";
        ObjectStream<String> lineStream = new PlainTextByLineStream(new StringReader(input));

        perfMon.start();
        String line;
        String whitespaceTokenizerLine[] = null;

        String[] tags = null;
        while ((line = lineStream.read()) != null) {
            whitespaceTokenizerLine = WhitespaceTokenizer.INSTANCE
                    .tokenize(line);
            tags = tagger.tag(whitespaceTokenizerLine);

            POSSample sample = new POSSample(whitespaceTokenizerLine, tags);
            System.out.println(sample.toString());
            perfMon.incrementCounter();
        }
        perfMon.stopAndPrintFinalResult();

        // chunker
        InputStream is = new FileInputStream("en-chunker.bin");
        ChunkerModel cModel = new ChunkerModel(is);

        ChunkerME chunkerME = new ChunkerME(cModel);
        String result[] = chunkerME.chunk(whitespaceTokenizerLine, tags);

        for (String s : result) {
            System.out.println(s);
        }

        Span[] span = chunkerME.chunkAsSpans(whitespaceTokenizerLine, tags);
        for (Span s : span) {
            System.out.println(s.toString());
        }
    }

    public static void Parse() throws InvalidFormatException, IOException {
        // http://sourceforge.net/apps/mediawiki/opennlp/index.php?title=Parser#Training_Tool
        InputStream is = new FileInputStream("en-parser-chunking.bin");

        ParserModel model = new ParserModel(is);

        Parser parser = ParserFactory.create(model);

        String sentence = "Programcreek is a very huge and useful website.";
        Parse topParses[] = ParserTool.parseLine(sentence, parser, 1);

        for (Parse p : topParses) {
            p.show();
        }

        is.close();

        /*
	 * (TOP (S (NP (NN Programcreek) ) (VP (VBZ is) (NP (DT a) (ADJP (RB
	 * very) (JJ huge) (CC and) (JJ useful) ) ) ) (. website.) ) )
     */
    //}
}
