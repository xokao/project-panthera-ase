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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.parse.sql.SqlASTNode;
import org.apache.hadoop.hive.ql.parse.sql.SqlXlateException;
import org.apache.hadoop.hive.ql.parse.sql.TranslateContext;
/**
 * Do something before or after transform.
 * BaseSqlASTTransformer.
 *
 */
public abstract class BaseSqlASTTransformer implements SqlASTTransformer {
  private static final Log LOG = LogFactory.getLog(BaseSqlASTTransformer.class);


  @Override
  public void transformAST(SqlASTNode tree, TranslateContext context) throws SqlXlateException {

    transform(tree, context);

    //track log
    LOG.info("After "+ this.getClass().getSimpleName()+", sql ast is:"+tree.toStringTree());
    LOG.info("After "+ this.getClass().getSimpleName()+", query info is:"+context.getQInfoRoot().toStringTree());
    LOG.info("After "+ this.getClass().getSimpleName()+", filterBlock is:"+context.getQInfoRoot().toFilterBlockStringTree());
  }

  protected abstract void transform(SqlASTNode tree, TranslateContext context)
      throws SqlXlateException;

}
