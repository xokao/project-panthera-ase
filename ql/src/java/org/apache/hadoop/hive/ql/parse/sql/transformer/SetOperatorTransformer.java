/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.parse.sql.transformer;

import java.util.List;

import org.antlr33.runtime.tree.CommonTree;
import org.apache.hadoop.hive.ql.parse.sql.PantheraExpParser;
import org.apache.hadoop.hive.ql.parse.sql.SqlXlateException;
import org.apache.hadoop.hive.ql.parse.sql.SqlXlateUtil;
import org.apache.hadoop.hive.ql.parse.sql.TranslateContext;
import org.apache.hadoop.hive.ql.parse.sql.transformer.fb.FilterBlockUtil;

import br.com.porcelli.parser.plsql.PantheraParser_PLSQLParser;

/**
 * Base class for IntersectTransformer & MinusTransformer
 *
 * SetOperatorTransformer.
 *
 */
public abstract class SetOperatorTransformer extends BaseSqlASTTransformer {

  void processIntersect(CommonTree setOperator, TranslateContext context) throws SqlXlateException {
    int intersectIndex = setOperator.getChildIndex();
    if (intersectIndex != 1) {
      throw new SqlXlateException(setOperator, "unsupported " + setOperator.getText() + " type!");
    }
    CommonTree parent = (CommonTree) setOperator.getParent();
    CommonTree leftSelect = (CommonTree) parent.getChild(0);
    CommonTree rightSelect = (CommonTree) setOperator.getChild(0);
    CommonTree leftSelectList = (CommonTree) leftSelect
        .getFirstChildWithType(PantheraExpParser.SELECT_LIST);
    CommonTree rightSelectList = (CommonTree) rightSelect
        .getFirstChildWithType(PantheraExpParser.SELECT_LIST);
    if (leftSelectList == null || rightSelectList == null) {
      // FIXME *
      throw new SqlXlateException(setOperator, "unsupported SELECT * in " + setOperator.getText() + "!");
    }
    if (leftSelectList.getChildCount() != rightSelectList.getChildCount()) {
      throw new SqlXlateException(setOperator, "incosistent columns for " + setOperator.getText() + "!");
    }
    List<CommonTree> leftColumnAliasList = FilterBlockUtil.buildSelectListAlias(leftSelectList,
        context);
    List<CommonTree> rightColumnAliasList = FilterBlockUtil.buildSelectListAlias(rightSelectList,
        context);

    CommonTree leftTableRefElement = FilterBlockUtil.createTableRefElement(leftSelect, context);
    CommonTree rightTableRefElement = FilterBlockUtil.createTableRefElement(rightSelect, context);
    CommonTree on = makeOn(setOperator, leftTableRefElement, rightTableRefElement, leftColumnAliasList,
        rightColumnAliasList);

    CommonTree select = makeJoin(leftTableRefElement, rightTableRefElement, on, context);
    CommonTree distinct = FilterBlockUtil.createSqlASTNode(
        setOperator, PantheraExpParser.SQL92_RESERVED_DISTINCT, "distinct");
    select.addChild(distinct);
    CommonTree closingSelectList = FilterBlockUtil.createSelectList(select, leftColumnAliasList);
    select.addChild(closingSelectList);
    CommonTree where = makeWhere(setOperator, leftTableRefElement, rightTableRefElement, leftColumnAliasList,
        rightColumnAliasList);
    if (where != null) {
      select.addChild(where);
    }
    parent.deleteChild(0);
    parent.deleteChild(0);
    SqlXlateUtil.addCommonTreeChild(parent, 0, select);
  }

  private CommonTree makeJoin(CommonTree leftTableRefElement, CommonTree rightTableRefElement,
      CommonTree on, TranslateContext context) {
    CommonTree select = FilterBlockUtil.createSqlASTNode(on, PantheraExpParser.SQL92_RESERVED_SELECT,
        "select");
    CommonTree from = FilterBlockUtil.createSqlASTNode(on, PantheraExpParser.SQL92_RESERVED_FROM,
        "from");
    select.addChild(from);
    CommonTree tableRef = FilterBlockUtil
        .createSqlASTNode(on, PantheraExpParser.TABLE_REF, "TABLE_REF");
    from.addChild(tableRef);
    tableRef.addChild(leftTableRefElement);
    CommonTree join = makeJoinNode(on);
    tableRef.addChild(join);
    join.addChild(rightTableRefElement);
    join.addChild(on);
    return select;
  }

  abstract CommonTree makeJoinNode(CommonTree on);

  abstract CommonTree makeWhere(CommonTree setOperator, CommonTree leftTableRefElement, CommonTree rightTableRefElement,
      List<CommonTree> leftColumnAliasList, List<CommonTree> rightColumnAliasList);

  private CommonTree makeOn(CommonTree setOperator, CommonTree leftTableRefElement, CommonTree rightTableRefElement,
      List<CommonTree> leftColumnAliasList, List<CommonTree> rightColumnAliasList) {
    CommonTree on = FilterBlockUtil.createSqlASTNode(setOperator, PantheraParser_PLSQLParser.SQL92_RESERVED_ON,
        "on"); //should be create from setOperator
    CommonTree logicExpr = FilterBlockUtil.createSqlASTNode(setOperator, PantheraParser_PLSQLParser.LOGIC_EXPR,
        "LOGIC_EXPR");
    on.addChild(logicExpr);
    int count = leftColumnAliasList.size();
    String leftAlias = leftTableRefElement.getChild(0).getChild(0).getText();
    String rightAlias = rightTableRefElement.getChild(0).getChild(0).getText();
    for (int i = 0; i < count; i++) {
      CommonTree condition = makeEqualCondition(on, leftAlias, rightAlias, leftColumnAliasList.get(i)
          .getChild(0).getText(), rightColumnAliasList.get(i).getChild(0).getText());
      if (logicExpr.getChildCount() == 0) {
        logicExpr.addChild(condition);
      } else {
        CommonTree and = FilterBlockUtil.createSqlASTNode(setOperator, PantheraExpParser.SQL92_RESERVED_AND,
            "and");
        CommonTree leftChild = (CommonTree) logicExpr.deleteChild(0);
        and.addChild(leftChild);
        and.addChild(condition);
        logicExpr.addChild(and);
      }
    }
    return on;
  }

  private CommonTree makeEqualCondition(CommonTree on, String leftAlias, String rightAlias, String leftColumn,
      String rightColumn) {
    CommonTree equal = FilterBlockUtil.createSqlASTNode(on, PantheraExpParser.EQUALS_OP, "=");
    equal.addChild(FilterBlockUtil.createCascatedElementBranch(equal, leftAlias, leftColumn));
    equal.addChild(FilterBlockUtil.createCascatedElementBranch(equal, rightAlias, rightColumn));
    return equal;

  }
}
