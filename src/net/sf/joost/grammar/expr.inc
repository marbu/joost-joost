/*
 * $Id: expr.inc,v 1.1 2002/08/27 09:40:51 obecker Exp $
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

/* Expression grammar which is needed for expressions (sic!) as well as
   for patterns (different start symbols) */

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
   STRING:n
      {: RESULT = new Tree(Tree.STRING, n); :}
 | NUMBER:n
      {: RESULT = new Tree(Tree.NUMBER, (Double)n); :}
 | LPAR Expr:t RPAR
      {: RESULT = t; :}
 | TEXT
      {: RESULT = new Tree(Tree.TEXT_TEST, null); :}
 | AT Name:n
      {: RESULT = new Tree(Tree.ATTR, (String)n, 
                           parser.j_nsSet, parser.j_locator); :}
 | NodeExpr:t
      {: RESULT = t; :}
 | NodeExpr:e SLASH AT Name:n
      {: RESULT = new Tree(Tree.ATTR, (String)n, 
                           parser.j_nsSet, parser.j_locator); 
         ((Tree)RESULT).left = (Tree)e; :}
 | ParentPath:t
      {: RESULT = t; :}
 | ParentPath:p SLASH AT Name:n
      {: RESULT = new Tree(Tree.ATTR, (String)n, 
                           parser.j_nsSet, parser.j_locator); 
         ((Tree)RESULT).left = (Tree)p; :}
 ;

NodeExpr ::=
   FunctionCall:t
      {: RESULT = t; :}
 | DOLLAR Name:n
      {: RESULT = new Tree(Tree.VAR, (String)n, 
                           parser.j_nsSet, parser.j_locator); :}
 | DOT
      {: RESULT = new Tree(Tree.DOT, null); :}
 ;

ParentPath ::=
   DDOT
      {: RESULT = new Tree(Tree.DDOT, null); :}
 | ParentPath:p SLASH DDOT
      {: RESULT = new Tree(Tree.DDOT, (Tree)p, null); :}
 ;

FunctionCall ::=
   Name:n LPAR ExprList:p RPAR
      {: RESULT = new Tree(Tree.FUNCTION, (String)n, (Tree)p, 
                           parser.j_nsSet, parser.j_locator); :}
 ;

ExprList ::=
 | Expr:t
      {: RESULT = t; :}
 | ExprList:t1 COMMA Expr:t2
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

