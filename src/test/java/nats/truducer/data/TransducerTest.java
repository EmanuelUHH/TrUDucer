package nats.truducer.data;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import nats.truducer.io.ruleparsing.TransducerParser;
import org.junit.Assert;
import org.junit.Test;

import static nats.truducer.TestUtils.stringToParser;
import static nats.truducer.TestUtils.stringToTree;

/**
 * Created by felix on 30/01/17.
 */
public class TransducerTest {

    // 1       Konkursgerüchte Gerücht N       NN      gender=neut|number=pl|case=nom|person=third     2       SUBJ    _       _
    // 2       drücken drücken V       VVFIN   number=pl|person=third|tense=present|mood=indicative    0       S       _       _
    // 3       Kurs    Kurs    N       NN      gender=masc|number=sg|case=acc|person=third             2       OBJA    _       _
    // 4       der     der     ART     ART     number=sg|case=gen|gender=fem                           5       DET     _       _
    // 5       Amazon-Aktie    Aktie   N       NN      gender=fem|number=sg|case=gen|person=third      3       GMOD    _       _
    private String testSentence =
        "# sent_id = 1\n" +
        "1\tKonkursgerüchte\tGerücht\tN\tNN\tgender=neut|number=pl|case=nom|person=third\t2\tSUBJ\t_\t_\n" +
        "2\tdrücken\tdrücken\tV\tVVFIN\tnumber=pl|person=third|tense=present|mood=indicative\t0\tS\t_\t_\n" +
        "3\tKurs\tKurs\tN\tNN\tgender=masc|number=sg|case=acc|person=third\t2\tOBJA\t_\t_\n" +
        "4\tder\tder\tART\tART\tnumber=sg|case=gen|gender=fem\t5\tDET\t_\t_\n" +
        "5\tAmazon-Aktie\tAktie\tN\tNN\tgender=fem|number=sg|case=gen|person=third\t3\tGMOD\t_\t_";

    private String testTransducer =
            "parent({n1:SUBJ(?ns), ?r}, ?rx) -> parent(n1:nsubj(?ns), ?r, ?rx);" + // TODO it should not be necessary to write that much. rest needs to be handled implicitly
                    "n1:S(?ns)    -> n1:root(?ns);" +
                    "n1:OBJA(?ns) -> n1:dobj(?ns);" +
                    "n1:DET(?ns)  -> n1:det(?ns);" +
                    "n1:GMOD(?ns) -> n1:nmod(?ns);";

//    1       Das     Das     ART     ART     number=sg|case=nom|gender=neut  2       DET     _       _
//    2       Thema   Thema   N       NN      number=sg|case=nom_dat_acc|person=third|gender=neut     3       SUBJ    _       _
//    3       werde   werden  V       VAFIN   tense=present|mood=subjunctive1|number=sg|person=third  0       S       _       _
//    4       jedoch  jedoch  ADV     ADV     cat2=KON|subcat=sentence        13      ADV     _       _
//    5       in      in      PREP    APPR    case=dat        13      PP      _       _
//    6       dieser  dies    ART     PDAT    number=sg|case=dat|gender=fem   7       DET     _       _
//    7       Legislaturperiode       Periode N       NN      number=sg|person=third|gender=fem       5       PN      _       _
//    8       sicherlich      sicherlich      ADV     ADV     subcat=sentence 13      ADV     _       _
//    9       wieder  wieder  ADV     ADV     subcat=temporal 13      ADV     _       _
//    10      auf     auf     PREP    APPR    case=acc        13      PP      _       _
//    11      den     den     ART     ART     number=sg|case=acc|gender=masc  12      DET     _       _
//    12      Tisch   Tisch   N       NN      number=sg|case=nom_dat_acc|person=third|gender=masc     10      PN      _       _
//    13      kommen  kommen  V       VVINF   _       3       AUX     _       _
//    14      .       .       $.      $.      _       0       ROOT    _       _

    private String testSentence2 =
    "# sent_id = 1\n" +
    "1\tDas\tDas\tART\tART\tnumber=sg|case=nom|gender=neut\t2\tDET\t_\t_\n" +
    "2\tThema\tThema\tN\tNN\tnumber=sg|case=nom_dat_acc|person=third|gender=neut\t3\tSUBJ\t_\t_\n" +
    "3\twerde\twerden\tV\tVAFIN\ttense=present|mood=subjunctive1|number=sg|person=third\t0\tS\t_\t_\n" +
    "4\tjedoch\tjedoch\tADV\tADV\tcat2=KON|subcat=sentence\t13\tADV\t_\t_\n" +
    "5\tin\tin\tPREP\tAPPR\tcase=dat\t13\tPP\t_\t_\n" +
    "6\tdieser\tdies\tART\tPDAT\tnumber=sg|case=dat|gender=fem\t7\tDET\t_\t_\n" +
    "7\tLegislaturperiode\tPeriode\tN\tNN\tnumber=sg|person=third|gender=fem\t5\tPN\t_\t_\n" +
    "8\tsicherlich\tsicherlich\tADV\tADV\tsubcat=sentence\t13\tADV\t_\t_\n" +
    "9\twieder\twieder\tADV\tADV\tsubcat=temporal\t13\tADV\t_\t_\n" +
    "10\tauf\tauf\tPREP\tAPPR\tcase=acc\t13\tPP\t_\t_\n" +
    "11\tden\tden\tART\tART\tnumber=sg|case=acc|gender=masc\t12\tDET\t_\t_\n" +
    "12\tTisch\tTisch\tN\tNN\tnumber=sg|case=nom_dat_acc|person=third|gender=masc\t10\tPN\t_\t_\n" +
    "13\tkommen\tkommen\tV\tVVINF\t_\t3\tAUX\t_\t_\n";

    private String testTransducer2 =
            "parent({n1:SUBJ(?ns), ?r}, ?rx) -> parent(n1:nsubj(?ns), ?r, ?rx);" +
            "n1:S(?ns)    -> n1:root(?ns);" +
            "n1:DET(?ns)  -> n1:det(?ns);" +
            "parent:$x({n:AUX(?auxr)}, ?r) -> n:$x(parent:aux(), ?auxr, ?r);" +
            "parent({n1:ADV(?ns), ?r}, ?rx) -> parent(n1:advmod(?ns), ?r, ?rx);" +
            "parent({n1:PP(n2:PN(?r2), ?r1), ?fr}, ?r) -> parent(n2:obl(n1:case(), ?r1, ?r2), ?fr, ?r);";
    //       n1:PP(n2:PN(?r2), ?r1) -> n2:obl(n1:case(), ?r1, ?r2)  // implicit top frontier node, implied rest nodes
    //       n1:SUBJ() -> n1:nsubj()   // implied rest nodes
    // -> consequence:  impossible to disallow childnodes
    //                  impossible to disallow neighboring nodes
    //        should be still possible with groovy code somehow

    private String testTransducer2b =
            "n() -> n:nsubj() :- {n.getDeprel() == \"SUBJ\"};" +
            "n1:S()    -> n1:root();" +
            "n1:DET()  -> n1:det() :- {_n1.setFeats(\"featvalue\")};" +
            "parent:$x({n:AUX(?auxr)}, ?r) -> n:$x(parent:aux(), ?auxr, ?r);" +
            "n:ADV() -> n:advmod();" +
            "n1:PP(n2:PN(?r2), ?r1) -> n2:obl(n1:case(), ?r1, ?r2)";

    private String testSentence3 =
            "# sent_id = 1\n" +
            "1\tIch\tx\tN\tNN\t\t2\tSUBJ\t_\t_\n" +
            "2\tkaufe\tx\tV\tVVFIN\t\t0\tS\t_\t_\n" +
            "3\tChristian\tx\tN\tNE\t\t2\tOBJD\t_\t_\n" +
            "4\tein\tx\tART\tART\t\t5\tDET\t_\t_\n" +
            "5\tBuch\tx\tN\tNN\t\t2\tOBJA\t_\t_";

    private String testTransducer3a =
            "n1:S(?ns)    -> n1:root(?ns);" +
            "parent({n1:SUBJ(?ns), ?r}, ?rx) -> parent(n1:nsubj(?ns), ?r, ?rx);" +
            "n1:DET(?ns)  -> n1:det(?ns);" +
            "parent({n1:OBJA(?r1), n2:OBJD(?r2)}, ?r3) -> parent(n1:obj(?r1), n2:iobj(?r2), ?r3);";

    private String testTransducer3b =
            "n1:S(?ns)    -> n1:root(?ns);" +
            "parent({n1:SUBJ(?ns), ?r}, ?rx) -> parent(n1:nsubj(?ns), ?r, ?rx);" +
            "n1:DET(?ns)  -> n1:det(?ns);" +
            "parent({n1:OBJA(?r1), n2:OBJD(?r2)}, ?r3) -> parent(n1(?r1), n2:iobj(?r2), ?r3);" +
            "n1:OBJD(?r) -> n1:obj(?r);" +
            "n1:OBJA(?r) -> n1:obj(?r);";

    private String testTransducer3c =
            "expansion subjDep = [SUBJ];\n" +
            "n1:S(?ns)    -> n1:root(?ns);" +
            "parent({n1:subjDep(?ns), ?r}, ?rx) -> parent(n1:nsubj(?ns), ?r, ?rx);" +
            "n1:DET(?ns)  -> n1:det(?ns);" +
            "parent({n1:OBJA(?r1), n2:OBJD(?r2)}, ?r3) -> parent(n1:obj(?r1), n2:iobj(?r2), ?r3);";

    // groovy test
    private String testTransducer3d =
            "n:S() -> n:root();" +
            "p({n()}) -> p(n:left()) :- { n.getOrd() < p.getOrd() };" +
            "n() -> n:right();";


    private String testSentence4 =
            "1\t(\t(\t$(\t$(\t_\t0\tROOT\t_\t_\n" +
            "2\tChristiane\tChristiane\tN\tNE\tgender=fem|person=third|number=sg|subcat=Vorname\t0\tS\t_\t_\n" +
            "3\tSchulzki-Haddouti\tSchulzki-Haddouti\tN\tNE\tpattern=NE|person=third\t2\tAPP\t_\t_\n" +
            "4\t)\t)\t$(\t$(\t_\t0\tROOT\t_\t_\n";

    private String testTransducer4 =
            "n:ROOT() -> n:ROOT();" +
            "n:S() -> n:root();" +
            "parent.NE({n.NE:APP(?r)}) -> parent(n:flat(?r));";

    private String testSentence5 =
            "1\tbis\tx\tx\tAPPR\t\t5\tPP\t\t\n" +
            "2\tzum\tx\tx\tAPPRART\t\t1\tPN\t\t\n" +
            "3\tOktober\tx\tx\tNN\t\t2\tPN\t\t\n" +
            "4\t2003\tx\tx\tCARD\t\t3\tZEIT\t\t\n" +
            "5\tsoll\tx\tx\tVMFIN\t\t0\tS\t\t\n";

    private String testTransducer5 =
            "n:S() -> n:root();" +
            "n1:PP(n2:PN(n3:PN())) -> n3(n1(), n2());" +
            "n1:PP(n2:PN(), ?r) -> n2:obl(n1:case(), ?r);" +
            "n:PN"; // TODO




    private String commentTestTransducer =
            "# comment\n" +
            "n1:SUBJ(?ns) -> n1:nsubj(?ns);";


    private Node getNode(Root tree, int id) {
        return tree.getDescendants().stream().filter(n -> n.getOrd() == id).findFirst().get();
    }

    @Test
    public void testTransducer1() {
        TransducerParser parser = stringToParser(testTransducer);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence));

        Assert.assertEquals("nsubj", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("root", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("dobj", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 4).getDeprel());
        Assert.assertEquals("nmod", getNode(newTree, 5).getDeprel());
    }

    @Test
    public void testTransducer2() {
        TransducerParser parser = stringToParser(testTransducer2);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence2));

        Assert.assertEquals("root", getNode(newTree, 13).getDeprel());
        Assert.assertEquals("nsubj", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("aux", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("advmod", getNode(newTree, 4).getDeprel());
        Assert.assertEquals("obl", getNode(newTree, 7).getDeprel());
        Assert.assertEquals("case", getNode(newTree, 5).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 6).getDeprel());
        Assert.assertEquals("advmod", getNode(newTree, 8).getDeprel());
        Assert.assertEquals("advmod", getNode(newTree, 9).getDeprel());
        Assert.assertEquals("obl", getNode(newTree, 12).getDeprel());
        Assert.assertEquals("case", getNode(newTree, 10).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 11).getDeprel());
    }

    @Test
    public void testTransducer2b() {
        TransducerParser parser = stringToParser(testTransducer2b);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence2));

        Assert.assertEquals("root", getNode(newTree, 13).getDeprel());
        Assert.assertEquals("nsubj", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("aux", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("advmod", getNode(newTree, 4).getDeprel());
        Assert.assertEquals("obl", getNode(newTree, 7).getDeprel());
        Assert.assertEquals("case", getNode(newTree, 5).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 6).getDeprel());
        Assert.assertEquals("advmod", getNode(newTree, 8).getDeprel());
        Assert.assertEquals("advmod", getNode(newTree, 9).getDeprel());
        Assert.assertEquals("obl", getNode(newTree, 12).getDeprel());
        Assert.assertEquals("case", getNode(newTree, 10).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 11).getDeprel());
        Assert.assertEquals("featvalue", getNode(newTree, 11).getFeats());
    }

    @Test
    public void testTransducer3a() {
        TransducerParser parser = stringToParser(testTransducer3a);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence3));

        Assert.assertEquals("root", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("nsubj", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 4).getDeprel());
        Assert.assertEquals("iobj", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("obj", getNode(newTree, 5).getDeprel());
    }

    /**
     * Tests if the rule priorities work.
     * The rule matching both OBJA and OBJD should be applied first,
     * as it apprears first.
     * The other 2 rules, concerning each OBJ seperately should be used as
     * fallback rules.
     */
    @Test
    public void testTransducer3b() {
        TransducerParser parser = stringToParser(testTransducer3b);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence3));

        Assert.assertEquals("root", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("nsubj", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 4).getDeprel());
        Assert.assertEquals("iobj", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("obj", getNode(newTree, 5).getDeprel());
    }

    /**
     * Test expansion lists
     */
    @Test
    public void testTransducer3c() {
        TransducerParser parser = stringToParser(testTransducer3c);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence3));

        Assert.assertEquals("root", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("nsubj", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("det", getNode(newTree, 4).getDeprel());
        Assert.assertEquals("iobj", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("obj", getNode(newTree, 5).getDeprel());
    }

    public void testTransducer3d() {
        TransducerParser parser = stringToParser(testTransducer3c);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence3));

        Assert.assertEquals("root", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("left", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("left", getNode(newTree, 4).getDeprel());
        Assert.assertEquals("right", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("right", getNode(newTree, 5).getDeprel());
    }

    @Test
    public void testTransducer4() {
        TransducerParser parser = stringToParser(testTransducer4);
        Transducer t = parser.transducer().t;

        Root newTree = t.applyTo(stringToTree(testSentence4));

        Assert.assertEquals("flat", getNode(newTree, 3).getDeprel());
        Assert.assertEquals("root", getNode(newTree, 2).getDeprel());
        Assert.assertEquals("ROOT", getNode(newTree, 1).getDeprel());
        Assert.assertEquals("ROOT", getNode(newTree, 4).getDeprel());
    }
    @Test
    public void testComment() {
        TransducerParser parser = stringToParser(commentTestTransducer);
        Transducer t = parser.transducer().t;

        Assert.assertEquals(1, t.rules.size());
    }

}
