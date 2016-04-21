
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
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
import opennlp.tools.util.Span;

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
    public static HashMap itemsList;

    public NLP() throws FileNotFoundException, IOException {
        listeFruits.add("pommes");
        listeFruits.add("poires");
        listeFruits.add("bananes");
        
        itemsList = new HashMap();
        
        model = new POSModelLoader().load(new File("fr-pos.bin"));
        perfMon = new PerformanceMonitor(System.err, "sent");
        tagger = new POSTaggerME(model);

        try (InputStream is = new FileInputStream("en-sent.bin")) {
            sentenceDetector = new SentenceDetectorME(new SentenceModel(is));
        } catch (Exception e) {
            System.out.println(e);
        }

        try (InputStream is = new FileInputStream("fr-token.bin")) {
            tokenizer = new TokenizerME(new TokenizerModel(is));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String Tokenize(String phrase) throws InvalidFormatException, IOException {
        POSTag(phrase);
        tokens = tokenizer.tokenize(phrase);
        
        for (String token : tokens) {
            if ("prix".equals(token) || "coûtent".equals(token)) {
                return getPrix();
            }
        }
        return "Je ne comprends pas.";
    }

    public static String getPrix() {
        String output = "";
        for (String token : tokens) {
            if (listeFruits.contains(token)) {
                output = "Le prix des " + token + " est de " + getValue(token) + "€.";
            }
        }
        return output;
    }

    public static int getValue(String token) {
        switch (token) {
            case "pommes":
                return 5;
            case "poires":
                return 5;
            case "bananes":
                return 5;
            default:
                return 10;
        }
    }

    public static void POSTag(String input) throws IOException {

        ObjectStream<String> lineStream = new PlainTextByLineStream(new StringReader(input));

        tags = tagger.tag(WhitespaceTokenizer.INSTANCE.tokenize(lineStream.read()));
        
        for (String tag : tags) {
            System.out.println(tag);
        }
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
