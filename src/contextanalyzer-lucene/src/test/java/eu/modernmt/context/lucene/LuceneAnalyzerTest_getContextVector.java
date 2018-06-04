//package eu.modernmt.context.lucene;
//
//import eu.modernmt.context.ContextAnalyzerException;
//import eu.modernmt.lang.Language;
//import eu.modernmt.lang.LanguagePair;
//import eu.modernmt.model.ContextVector;
//import eu.modernmt.model.Memory;
//import eu.modernmt.model.corpus.MultilingualCorpus;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import static eu.modernmt.context.lucene.TestData.*;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * Created by davide on 07/08/17.
// */
//public class LuceneAnalyzerTest_getContextVector {
//
//    private TLuceneAnalyzer analyzer;
//
//    @Before
//    public void setup() throws Throwable {
//        this.analyzer = new TLuceneAnalyzer(EN__IT, IT__EN, EN__FR);
//
//        String enHelloWorld = "hello world 1\nhello world 2";
//        String enTheTest = "the test 1\nthe test 2";
//        String itHelloWorld = "ciao mondo 1\nciao mondo 2";
//        String itTheTest = "il test 1\nil test 2";
//        String frHelloWorld = "bonjour monde 1\nbonjour monde 2";
//        String frTheTest = "le preuve 1\nle preuve 2";
//
//        DummyBilingualCorpus enItHelloWorld = TestData.corpus("none", EN__IT, enHelloWorld, itHelloWorld);
//        DummyBilingualCorpus enFrHelloWorld = TestData.corpus("none", EN__FR, enHelloWorld, frHelloWorld);
//        DummyBilingualCorpus enItTheTest = TestData.corpus("none", EN__IT, enTheTest, itTheTest);
//        DummyBilingualCorpus enFrTheTest = TestData.corpus("none", EN__FR, enTheTest, frTheTest);
//
//        MultilingualCorpus langMixedCorpus = TestData.corpus(enItHelloWorld, enFrHelloWorld);
//        MultilingualCorpus contentMixedCorpus = TestData.corpus(enItHelloWorld, enItTheTest);
//
//        this.analyzer.add(new Memory(1), enItHelloWorld);
//        this.analyzer.add(new Memory(2), enFrHelloWorld);
//        this.analyzer.add(new Memory(3), enItTheTest);
//        this.analyzer.add(new Memory(4), enFrTheTest);
//
//        this.analyzer.add(new Memory(12), langMixedCorpus);
//        this.analyzer.add(new Memory(13), contentMixedCorpus);
//
//        this.analyzer.flush();
//    }
//
//    @After
//    public void teardown() throws Throwable {
//        if (this.analyzer != null)
//            this.analyzer.close();
//        this.analyzer = null;
//    }
//
//    private static boolean contains(ContextVector result, long memory) {
//        for (ContextVector.Entry entry : result) {
//            if (entry.memory.getId() == memory)
//                return true;
//        }
//
//        return false;
//    }
//
//    private void test(LanguagePair lang, String query, int... memories) throws ContextAnalyzerException {
//        ContextVector result = analyzer.getContextVector(lang, query, 100);
//
//        assertEquals(memories == null ? 0 : memories.length, result.size());
//        if (memories != null) {
//            for (int memory : memories)
//                assertTrue(contains(result, memory));
//        }
//    }
//
//    @Test
//    public void directSearchWithItalianHelloWorld() throws Throwable {
//        test(EN__IT, "hello world", 1, 12, 13);
//    }
//
//    @Test
//    public void directSearchWithItalianTheTest() throws Throwable {
//        test(EN__IT, "the test", 3, 13);
//    }
//
//    @Test
//    public void directSearchWithItalianDialect() throws Throwable {
//        test(EN_US__IT_CH, "hello world", 1, 12, 13);
//    }
//
//    @Test
//    public void reversedSearchWithItalianHelloWorld() throws Throwable {
//        test(IT__EN, "ciao mondo", 1, 12, 13);
//    }
//
//    @Test
//    public void reversedSearchWithItalianTheTest() throws Throwable {
//        test(IT__EN, "il test", 3, 13);
//    }
//
//    @Test
//    public void reversedSearchWithItalianDialect() throws Throwable {
//        test(IT_CH__EN_US, "ciao mondo", 1, 12, 13);
//    }
//
//    @Test
//    public void directSearchWithFrenchHelloWorld() throws Throwable {
//        test(EN__FR, "hello world", 2, 12);
//    }
//
//    @Test
//    public void reversedSearchWithFrenchHelloWorld() throws Throwable {
//        test(FR__EN, "bonjour monde");
//    }
//
//}
