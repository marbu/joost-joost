/*
 * $Id: expr.inc,v 1.2 2003/01/05 21:21:12 obecker Exp $
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
non terminal Expr, AndExpr, GeneralComp;
non terminal AdditiveExpr, MultiplicativeExpr, UnaryExpr, BasicExpr;
non terminal Accessor, NodeAccessor, RelativeAccessor;
non terminal AccessorStep, AccessorAxis, NodeNameTest; 
non terminal PropAccessor, AttributeNameTest, NamespaceNameTest;
non terminal FunctionCall, ExprSeq, Name;


/* Start symbol, replaced by Ant with the grammar name (Pattern or Expr)*/
start with @@@START-NT@@@;


/* Grammar */
Expr ::= 
   Expr:e1 OR AndExpr:e2
      {: RESULT = new Tree(Tree.OR, (Tree)e1, (Tree)e2); :}
 | AndExpr:t
      {: RESULT = t; :}
 ;

AndExpr ::=
   AndExpr:e1 AND GeneralComp:e2
      {: RESULT = new Tree(Tree.AND, (Tree)e1, (Tree)e2); :}
 | GeneralComp:t
      {: RESULT = t; :}
 ;

/* recursiveness to discuss! */
GeneralComp ::=
   GeneralComp:e1 EQ AdditiveExpr:e2
      {: RESULT = new Tree(Tree.EQ, (Tree)e1, (Tree)e2); :}
 | GeneralComp:e1 NE AdditiveExpr:e2
      {: RESULT = new Tree(Tree.NE, (Tree)e1, (Tree)e2); :}
 | GeneralComp:e1 LT AdditiveExpr:e2
      {: RESULT = new Tree(Tree.LT, (Tree)e1, (Tree)e2); :}
 | GeneralComp:e1 LE AdditiveExpr:e2
      {: RESULT = new Tree(Tree.LE, (Tree)e1, (Tree)e2); :}
 | GeneralComp:e1 GT AdditiveExpr:e2
      {: RESULT = new Tree(Tree.GT, (Tree)e1, (Tree)e2); :}
 | GeneralComp:e1 GE AdditiveExpr:e2
      {: RESULT = new Tree(Tree.GE, (Tree)e1, (Tree)e2); :}
 | AdditiveExpr:t
      {: RESULT = t; :}
 ;

AdditiveExpr ::=
   AdditiveExpr:e1 PLUS MultiplicativeExpr:e2
      {: RESULT = new Tree(Tree.ADD, (Tree)e1, (Tree)e2); :}
 | AdditiveExpr:e1 MINUS MultiplicativeExpr:e2
      {: RESULT = new Tree(Tree.SUB, (Tree)e1, (Tree)e2); :}
 | MultiplicativeExpr:t
      {: RESULT = t; :}
 ;

MultiplicativeExpr ::=
   MultiplicativeExpr:e1 STAR UnaryExpr:e2
      {: RESULT = new Tree(Tree.MULT, (Tree)e1, (Tree)e2); :}
 | MultiplicativeExpr:e1 DIV UnaryExpr:e2
      {: RESULT = new Tree(Tree.DIV, (Tree)e1, (Tree)e2); :}
 | MultiplicativeExpr:e1 MOD UnaryExpr:e2
      {: RESULT = new Tree(Tree.MOD, (Tree)e1, (Tree)e2); :}
 | UnaryExpr:t
      {: RESULT = t; :}
 ;

UnaryExpr ::=
   PLUS BasicExpr:t
      {: RESULT = new Tree(Tree.ADD, null, (Tree)t); :}
 | MINUS BasicExpr:t
      {: RESULT = new Tree(Tree.SUB, null, (Tree)t); :}
 | BasicExpr:t
      {: RESULT = t; :}
 ;

BasicExpr ::=
   Accessor:t
      {: RESULT = t; :}
 | STRING:n  /* Literal */
      {: RESULT = new Tree(Tree.STRING, n); :}
 | NUMBER:n  /* Literal */
      {: RESULT = new Tree(Tree.NUMBER, (Double)n); :}
 | LPAR ExprSeq:t RPAR  /* ParenthesizedExpr */
      {: RESULT = t; :}
 | TEXT
      {: RESULT = new Tree(Tree.TEXT_TEST, null); :}
 ;

Accessor ::= 
   NodeAccessor:n SLASH PropAccessor:p
      {: ((Tree)p).left = (Tree)n; 
         RESULT = p; :} 
 | NodeAccessor:n
      {: RESULT = n; :}
 | RelativeAccessor:n SLASH PropAccessor:p
      {: ((Tree)p).left = (Tree)n; 
         RESULT = p; :} 
 | RelativeAccessor:n
      {: RESULT = n; :}
 | SLASH RelativeAccessor:n SLASH PropAccessor:p
      {: ((Tree)p).left = (Tree)n; 
         RESULT = p; :} 
 | SLASH RelativeAccessor:n
      {: RESULT = n; :}
 | DSLASH RelativeAccessor:n SLASH PropAccessor:p
      {: ((Tree)p).left = (Tree)n; 
         RESULT = p; :} 
 | DSLASH RelativeAccessor:n
      {: RESULT = n; :}
 | PropAccessor:p
      {: RESULT = p; :}
 ;

/* 
I'd rather specify something along the lines

Accessor ::=
   NodeAccessor SLASH PropAccessor
 | NodeAccessor
 | PropAccessor
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
      {: RESULT = new Tree(Tree.VAR, (String)n, 
                           parser.j_nsSet, parser.j_locator); :}
 | FunctionCall:t
      {: RESULT = t; :}
 ;

RelativeAccessor ::=
   RelativeAccessor:r SLASH AccessorStep:a
      {: Tree at = (Tree)a;
         if (at.type == Tree.DOT || at.type == Tree.DDOT) {
            at.left = (Tree)r;
            RESULT = at;
         }
         else
            RESULT = new Tree(Tree.CHILD, (Tree)r, at);
      :}
 | RelativeAccessor DSLASH AccessorStep
 | AccessorStep:a
      {: RESULT = a; :}
 ;

AccessorStep ::=
   AccessorAxis NodeNameTest:n 
      {: RESULT = n; :}
 | NodeNameTest:n
      {: RESULT = n; :}
 | DDOT
      {: RESULT = new Tree(Tree.DDOT, null); :}
 | DOT
      {: RESULT = new Tree(Tree.DOT, null); :}
 ;

AccessorAxis ::=
   PARENT
 | ANCESTOR
 ;

NodeNameTest ::=
   Name:n
      {: RESULT = new Tree(Tree.NAME_TEST, (String)n, 
                           parser.j_nsSet, parser.j_locator); :}
 | STAR
      {: RESULT = new Tree(Tree.WILDCARD, null); :}
 | STAR COLON NCNAME:n
      {: RESULT = new Tree(Tree.URI_WILDCARD, "*", (String)n, 
                           parser.j_nsSet, parser.j_locator); :}
 | NCNAME:n COLON STAR
      {: RESULT = new Tree(Tree.LOCAL_WILDCARD, (String)n, "*", 
                           parser.j_nsSet, parser.j_locator); :}
 ;

/* Property Accessor */
PropAccessor ::= 
   AttributeNameTest:t
      {: RESULT = t; :}
 | NamespaceNameTest
 ;

AttributeNameTest ::=
   AT Name:n
      {: RESULT = new Tree(Tree.ATTR, (String)n,
                           parser.j_nsSet, parser.j_locator); :}
 | AT STAR
      {: RESULT = new Tree(Tree.ATTR_WILDCARD, null); :}
 | AT STAR COLON NCNAME:n
      {: RESULT = new Tree(Tree.ATTR_URI_WILDCARD, "*", (String)n,
                           parser.j_nsSet, parser.j_locator); :}
 | AT NCNAME:n COLON STAR
      {: RESULT = new Tree(Tree.ATTR_LOCAL_WILDCARD, (String)n, "*",
                           parser.j_nsSet, parser.j_locator); :}
 ;

NamespaceNameTest ::=
   NAMESPACE NCNAME
 | NAMESPACE STAR
 ;

FunctionCall ::=
   Name:n LPAR ExprSeq:e RPAR
      {: RESULT = new Tree(Tree.FUNCTION, (String)n, (Tree)e,
                           parser.j_nsSet, parser.j_locator); :}
 ;

ExprSeq ::=
 | Expr:t
      {: RESULT = t; :}
 | ExprSeq:t1 COMMA Expr:t2
      {: RESULT = new Tree(Tree.LIST, (Tree)t1, (Tree)t2); :}
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
