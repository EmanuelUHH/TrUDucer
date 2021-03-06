grammar Transducer;
@header {
import nats.truducer.data.Rule;
import nats.truducer.data.Transducer;
import nats.truducer.data.Tree;
import nats.truducer.data.FrontierNode;
import nats.truducer.data.MatchingNode;
import nats.truducer.data.ReplacementNode;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
}

transducer returns [Transducer t] :
  { List<Rule> rules = new ArrayList<>();
    Map<String, List<String>> expansions = new HashMap<>(); }
  ( rx=convrule[expansions] {rules.add($rx.rule); }
  | ls=expansion {expansions.put($ls.key, $ls.value); }
  )*
  { $t = new Transducer(rules); };

expansion returns [String key, List<String> value] :
  'expansion' id=IDENTIFIER { $key=$id.getText(); } '='
  '['
    val1=IDENTIFIER {
      $value = new ArrayList<>();
      $value.add($val1.getText());
    }
    (',' val=IDENTIFIER { $value.add($val.getText()); } )*
  ']' ';';

convrule[Map<String, List<String>> expansions] returns [ Rule rule ] :
 mt=matchTree[expansions] '->' rt=replacementNode ( rb=RULEBODY )? ';'
 {
     String groovyCode = null;
     if ($rb != null) {
         groovyCode = $rb.getText();
     }
     $rule = new Rule($mt.tree, $rt.tree, groovyCode, _localctx.getText());
 };

edgeLabel returns [ String label ] :
  (dr=IDENTIFIER|dr=Q_IDENTIFIER) ( ':' extension=IDENTIFIER )?
  { $label = $dr.getText();
    if ($extension != null)
      $label += ":" + $extension.getText();
  };

replacementNode returns [ ReplacementNode tree ] :
 id=IDENTIFIER
 { ReplacementNode rn = new ReplacementNode();
   $tree = rn;
   rn.setName($id.getText());
 }
 ( ':' dr=edgeLabel
   {
     rn.setDepRel($dr.label);
   }
 )?
 '(' ( ( rt=replacementNode { rn.addChild($rt.tree);
                              $rt.tree.setParent(rn); }
       | '?' ca=IDENTIFIER { rn.addCatchAllVar($ca.getText()); } )
  (',' ( rt=replacementNode { rn.addChild($rt.tree);
                              $rt.tree.setParent(rn); }
       | '?' ca=IDENTIFIER { rn.addCatchAllVar($ca.getText()); } ) )* )? ')' ;

matchTree[Map<String, List<String>> expansions] returns [ Tree tree ] :
   n=node[expansions] { $tree=$n.tree; }
 | f=frontier[expansions] { $tree=$f.tree; }
 ;

node[Map<String, List<String>> expansions] returns [ Tree tree ] :
 { String elabel = null; }
 ( id=IDENTIFIER ( ':' dr=edgeLabel {elabel = $dr.label;})? ( '.' pos=IDENTIFIER )?
 | id=IDENTIFIER ( '.' pos=IDENTIFIER )? ( ':' dr=edgeLabel {elabel = $dr.label;})? )
 { MatchingNode mn = new MatchingNode($id.getText(),
                                      $pos != null ? $pos.getText() : null,
									  elabel,
                                      expansions);
   $tree = new Tree(mn); }
 '(' ( ( dt=matchTree[expansions] { mn.addChild($dt.tree.root);
                        $tree.frontierNode=$dt.tree.frontierNode;
                        $dt.tree.root.setParent(mn); }
       | '?' ca=IDENTIFIER { mn.setCatchallVar($ca.getText()); } )
  (',' ( dt=matchTree[expansions] { mn.addChild($dt.tree.root);
                      if ($dt.tree.frontierNode != null) {
                          if ($tree.frontierNode == null) {
                              $tree.frontierNode = $dt.tree.frontierNode;
                          } else {
                              throw new ParseCancellationException("Multiple Frontier Nodes detected");
                          }
                      }
                      $dt.tree.root.setParent(mn); }
       | '?' ca=IDENTIFIER { mn.setCatchallVar($ca.getText()); } ) )* )? ')' ;


frontier[Map<String, List<String>> expansions] returns [ Tree tree ] :
  { FrontierNode fn = new FrontierNode();
    $tree = new Tree(fn);
    $tree.frontierNode = fn; }
  '{' ( ( n=node[expansions] { fn.addChild($n.tree.root);
                   if ($n.tree.frontierNode != null)
                       throw new ParseCancellationException("Nested frontiers not allowed.");
                   $n.tree.root.setParent(fn); }
        | '?' ca=IDENTIFIER { fn.setCatchallVar($ca.getText()); }
        )
        (',' ( n=node[expansions] { fn.addChild($n.tree.root);
                         if ($n.tree.frontierNode != null)
                             throw new ParseCancellationException("Nested frontiers not allowed.");
                         $n.tree.root.setParent(fn); }
             | '?' ca=IDENTIFIER { fn.setCatchallVar($ca.getText()); }
             )
        )*
      )?
  '}' ;

RULEBODY : ':-' ' '+ GROOVY_BLOCK
 {
             String groovyCode = getText();
             groovyCode = groovyCode.substring(groovyCode.indexOf("{") + 1, groovyCode.length() - 1);
             setText(groovyCode);
 };

fragment GROOVY_BLOCK : '{' (~('{'|'}') | GROOVY_BLOCK)* '}';

IDENTIFIER : ('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'0'..'9')* ;

Q_IDENTIFIER : '$' ('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'0'..'9')*;

COMMENT : '#' (~('\n'|'\r')* '\r'? '\n' {skip();});

WS  : [ \t\r\n]+ -> skip ;
