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

package org.apache.beam.dsls.sql.rel;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import org.apache.beam.dsls.sql.planner.BeamSQLRelUtils;
import org.apache.beam.dsls.sql.schema.BeamSQLRecordType;
import org.apache.beam.dsls.sql.schema.BeamSQLRow;
import org.apache.beam.dsls.sql.schema.BeamSqlRowCoder;
import org.apache.beam.dsls.sql.schema.BeamTableUtils;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.core.Values;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;

/**
 * {@code BeamRelNode} to replace a {@code Values} node.
 *
 * <p>{@code BeamValuesRel} will be used in the following SQLs:
 * <ul>
 *   <li>{@code insert into t (name, desc) values ('hello', 'world')}</li>
 *   <li>{@code select 1, '1', LOCALTIME}</li>
 * </ul>
 */
public class BeamValuesRel extends Values implements BeamRelNode {

  public BeamValuesRel(
      RelOptCluster cluster,
      RelDataType rowType,
      ImmutableList<ImmutableList<RexLiteral>> tuples,
      RelTraitSet traits) {
    super(cluster, rowType, tuples, traits);

  }

  @Override public PCollection<BeamSQLRow> buildBeamPipeline(PCollectionTuple inputPCollections)
      throws Exception {
    List<BeamSQLRow> rows = new ArrayList<>(tuples.size());
    String stageName = BeamSQLRelUtils.getStageName(this);
    if (tuples.isEmpty()) {
      throw new IllegalStateException("Values with empty tuples!");
    }

    BeamSQLRecordType beamSQLRecordType = BeamSQLRecordType.from(this.getRowType());
    for (ImmutableList<RexLiteral> tuple : tuples) {
      BeamSQLRow row = new BeamSQLRow(beamSQLRecordType);
      for (int i = 0; i < tuple.size(); i++) {
        BeamTableUtils.addFieldWithAutoTypeCasting(row, i, tuple.get(i).getValue());
      }
      rows.add(row);
    }

    return inputPCollections.getPipeline().apply(stageName, Create.of(rows))
        .setCoder(new BeamSqlRowCoder(beamSQLRecordType));
  }
}