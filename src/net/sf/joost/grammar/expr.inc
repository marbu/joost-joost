/*
 * $Id: expr.inc,v 1.6 2003/03/20 13:14:40 obecker Exp $
 * 
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the 
 * License.
 *
 * The Original Code is: this file
 *
 * The Initial Developer of the Original Code is Oliver Becker.
 *
 * Portions created by  ______________________ 
 * are Copyright (C) ______ _______________________. 
 * All Rights Reserved.
 *
 * Contributor(s): ______________________________________. 
 */

/* Common expression grammar which is needed for expressions (sic!) in
 * Expr.cup as well as for patterns in Pattern.cup 
 * (different start symbols) */


/* Non-terminals */
non terminal Tree   Expr, AndExpr, GeneralComp,
                    AdditiveExpr, MultiplicativeExpr, UnaryExpr, BasicExpr, 
                    Accessor, NodeAccessor, RelativeAccessor, AccessorStep,
                    NodeNameTest, KindTest, AttributeNameTest, 
                    FunctionCall, ExprSeq, ParaSeq;
non terminal String Name;

/* Start symbol, replaced by Ant with the grammar name (Pattern or Expr)*/
start with @@@START-NT@@@;


/* Grammar */
Expr ::= 
   Expr:e1 OR AndExpr:e2
      {: RESULT = new Tree(Tree.OR, e1, e2); :}
 | AndExpr:t
      {: RESULT = t; :}
 ;

AndExpr ::=
   AndExpr:e1 AND GeneralComp:e2
      {: RESULT = new Tree(Tree.AND, e1, e2); :}
 | GeneralComp:t
      {: RESULT = t; :}
 ;

/* recursiveness to discuss! */
GeneralComp ::=
   GeneralComp:e1 EQ AdditiveExpr:e2
      {: RESULT = new Tree(Tree.EQ, e1, e2); :}
 | GeneralComp:e1 NE AdditiveExpr:e2
      {: RESULT = new Tree(Tree.NE, e1, e2); :}
 | GeneralComp:e1 LT AdditiveExpr:e2
      {: RESULT = new Tree(Tree.LT, e1, e2); :}
 | GeneralComp:e1 LE AdditiveExpr:e2
      {: RESULT = new Tree(Tree.LE, e1, e2); :}
 | GeneralComp:e1 GT AdditiveExpr:e2
      {: RESULT = new Tree(Tree.GT, e1, e2); :}
 | GeneralComp:e1 GE AdditiveExpr:e2
      {: RESULT = new Tree(Tree.GE, e1, e2); :}
 | AdditiveExpr:t
      {: RESULT = t; :}
 ;

AdditiveExpr ::=
   AdditiveExpr:e1 PLUS MultiplicativeExpr:e2
      {: RESULT = new Tree(Tree.ADD, e1, e2); :}
 | AdditiveExpr:e1 MINUS MultiplicativeExpr:e2
      {: RESULT = new Tree(Tree.SUB, e1, e2); :}
 | MultiplicativeExpr:t
      {: RESULT = t; :}
 ;

MultiplicativeExpr ::=
   MultiplicativeExpr:e1 STAR UnaryExpr:e2
      {: RESULT = new Tree(Tree.MULT, e1, e2); :}
 | MultiplicativeExpr:e1 DIV UnaryExpr:e2
      {: RESULT = new Tree(Tree.DIV, e1, e2); :}
 | MultiplicativeExpr:e1 MOD UnaryExpr:e2
      {: RESULT = new Tree(Tree.MOD, e1, e2); :}
 | UnaryExpr:t
      {: RESULT = t; :}
 ;

UnaryExpr ::=
   PLUS BasicExpr:t
      {: RESULT = new Tree(Tree.ADD, null, t); :}
 | MINUS BasicExpr:t
      {: RESULT = new Tree(Tree.SUB, null, t); :}
 | BasicExpr:t
      {: RESULT = t; :}
 ;

BasicExpr ::=
   Accessor:t
      {: RESULT = t; :}
 | STRING:n  /* Literal */
      {: RESULT = new Tree(Tree.STRING, n); :}
 | NUMBER:n  /* Literal */
      {: RESULT = new Tree(Tree.NUMBER, n); :}
 | LPAR ExprSeq:t RPAR  /* ParenthesizedExpr */
      {: RESULT = (t == null ? new Tree(Tree.LIST, null, null) : t); :}
 | DOT
      {: RESULT = new Tree(Tree.DOT, null); :}
 ;

Accessor ::= 
   NodeAccessor:n SLASH AttributeNameTest:a
      {: a.left = n; 
         RESULT = a; :} 
 | NodeAccessor:n
      {: RESULT = n; :}
 | RelativeAccessor:n SLASH AttributeNameTest:a
      {: a.left = n.reverseAssociativity(); 
         RESULT = a; :} 
 | RelativeAccessor:n
      {: RESULT = n.reverseAssociativity(); :}
 | SLASH RelativeAccessor:n SLASH AttributeNameTest:a
      {: a.left = new Tree(Tree.ROOT, n.reverseAssociativity(), null); 
         RESULT = a; :} 
 | SLASH RelativeAccessor:n
      {: RESULT = new Tree(Tree.ROOT, n.reverseAssociativity(), null); :}
 | DSLASH RelativeAccessor:n SLASH AttributeNameTest:a
      {: a.left = new Tree(Tree.ROOT, 
                           new Tree(Tree.DESC, null, 
                                               n.reverseAssociativity()), 
                           null);
         RESULT = a; :} 
 | DSLASH RelativeAccessor:n
      {: RESULT = new Tree(Tree.ROOT, 
                           new Tree(Tree.DESC, null, 
                                               n.reverseAssociativity()), 
                           null); :}
 | AttributeNameTest:a
      {: RESULT = a; :}
 ;

/* 
I'd rather specify something along the lines

Accessor ::=
   NodeAccessor SLASH AttributeNameTest
 | NodeAccessor
 | AttributeNameTest
 ;

NodeAccessor ::=
   PathAccessor
 | Variable
 | FunctionCall
 ;

PathAccessor ::= 
   SLASH RelativeAccessor
 | DSLASH RelativeAccessor
 | RelativeAccessor
 ;

Unfortunately this results in shift/reduce conflicts in CUP.
*/

NodeAccessor ::= 
   DOLLAR Name:n  /* Variable */
      {: RESULT = new Tree(Tree.VAR, n, 
                           parser.j_nsSet, parser.j_locator); :}
 | FunctionCall:t
      {: RESULT = t; :}
 ;

RelativeAccessor ::=
   RelativeAccessor:r SLASH AccessorStep:a
      {: if (a.type == Tree.DDOT) {
            a.left = r;
            RESULT = a;
         }
         else
            RESULT = new Tree(Tree.CHILD, r, a);
      :}
 | RelativeAccessor:r DSLASH AccessorStep:a
      {: Tree d = new Tree(Tree.DESC, r, null);
         if (a.type == Tree.DDOT) {
            a.left = d;
            RESULT = a;
         }
         else 
            RESULT = new Tree(Tree.CHILD, d, a);
      :}
 | AccessorStep:a
      {: if (a.type == Tree.DDOT)
            RESULT = a;
         else
            RESULT = new Tree(Tree.CHILD, null, a);
      :}
 ;

AccessorStep ::=
   NodeNameTest:n
      {: RESULT = n; :}
 | KindTest:k
      {: RESULT = k; :}
 | DDOT
      {: RESULT = new Tree(Tree.DDOT, null); :}
 ;

NodeNameTest ::=
   Name:n
      {: RESULT = new Tree(Tree.NAME_TEST, n, 
                           parser.j_nsSet, parser.j_locator); :}
 | STAR
      {: RESULT = new Tree(Tree.WILDCARD, null); :}
 | STAR COLON NCNAME:n
      {: RESULT = new Tree(Tree.URI_WILDCARD, "*", n, 
                           parser.j_nsSet, parser.j_locator); :}
 | NCNAME:n COLON STAR
      {: RESULT = new Tree(Tree.LOCAL_WILDCARD, n, "*", 
                           parser.j_nsSet, parser.j_locator); :}
 ;

KindTest ::=
   COMMENT
      {: RESULT = new Tree(Tree.COMMENT_TEST, null); :}
 | NODE
      {: RESULT = new Tree(Tree.NODE_TEST, null); :}
 | PI_START RPAR
      {: RESULT = new Tree(Tree.PI_TEST, ""); :}
 | PI_START STRING:s RPAR
      {: RESULT = new Tree(Tree.PI_TEST, s); :}
 | TEXT
      {: RESULT = new Tree(Tree.TEXT_TEST, null); :}
 | CDATA
      {: RESULT = new Tree(Tree.CDATA_TEST, null); :}
 ;

AttributeNameTest ::=
   AT Name:n
      {: RESULT = new Tree(Tree.ATTR, n,
                           parser.j_nsSet, parser.j_locator); :}
 | AT STAR
      {: RESULT = new Tree(Tree.ATTR_WILDCARD, null); :}
 | AT STAR COLON NCNAME:n
      {: RESULT = new Tree(Tree.ATTR_URI_WILDCARD, "*", n,
                           parser.j_nsSet, parser.j_locator); :}
 | AT NCNAME:n COLON STAR
      {: RESULT = new Tree(Tree.ATTR_LOCAL_WILDCARD, n, "*",
                           parser.j_nsSet, parser.j_locator); :}
 ;

FunctionCall ::=
   Name:n LPAR ParaSeq:e RPAR
      {: RESULT = new Tree(Tree.FUNCTION, n, e,
                           parser.j_nsSet, parser.j_locator); :}
 ;

ExprSeq ::=
      {: RESULT = new Tree(Tree.SEQ, null, null); :}
 | Expr:t
      {: RESULT = t; :}
 | Expr:t1 COMMA ExprSeq:t2
      {: RESULT = new Tree(Tree.SEQ, t1, t2); :}
 ;

ParaSeq ::=
   /* empty: RESULT = null */
 | Expr:t
      {: RESULT = t; :}
 | ParaSeq:t1 COMMA Expr:t2
      {: RESULT = new Tree(Tree.LIST, t1, t2); :}
 ;

/* some keywords can be names too */
Name ::=
   QNAME:n
     {: RESULT = n; :}
 | NCNAME:n
     {: RESULT = n; :}
 | DIV:n
     {: RESULT = n; :}
 | MOD:n
     {: RESULT = n; :}
 | AND:n
     {: RESULT = n; :}
 | OR:n
     {: RESULT = n; :}
;
