/**
 *    Copyright 2016-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.dynamic.sql.insert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mybatis.dynamic.sql.SqlBuilder.insert;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

import org.junit.jupiter.api.Test;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.insert.render.FieldAndValue;
import org.mybatis.dynamic.sql.insert.render.FieldAndValueAndParameters;
import org.mybatis.dynamic.sql.insert.render.FieldAndValueAndParametersCollector;
import org.mybatis.dynamic.sql.insert.render.FieldAndValueCollector;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategies;

class InsertStatementTest {

    private static final SqlTable foo = SqlTable.of("foo");
    private static final SqlColumn<Integer> id = foo.column("id", JDBCType.INTEGER);
    private static final SqlColumn<String> firstName = foo.column("first_name", JDBCType.VARCHAR);
    private static final SqlColumn<String> lastName = foo.column("last_name", JDBCType.VARCHAR);
    private static final SqlColumn<String> occupation = foo.column("occupation", JDBCType.VARCHAR);
    
    @Test
    void testFullInsertStatementBuilder() {

        TestRecord record = new TestRecord();
        record.setLastName("jones");
        record.setOccupation("dino driver");
        
        InsertStatementProvider<TestRecord> insertStatement = insert(record)
                .into(foo)
                .map(id).toProperty("id")
                .map(firstName).toProperty("firstName")
                .map(lastName).toProperty("lastName")
                .map(occupation).toProperty("occupation")
                .build()
                .render(RenderingStrategies.MYBATIS3);
        
        String expectedStatement = "insert into foo "
                + "(id, first_name, last_name, occupation) "
                + "values (#{record.id,jdbcType=INTEGER}, #{record.firstName,jdbcType=VARCHAR}, #{record.lastName,jdbcType=VARCHAR}, #{record.occupation,jdbcType=VARCHAR})";
        
        assertThat(insertStatement.getInsertStatement()).isEqualTo(expectedStatement);
    }

    @Test
    void testInsertStatementBuilderWithNulls() {

        TestRecord record = new TestRecord();
        
        InsertStatementProvider<TestRecord> insertStatement = insert(record)
                .into(foo)
                .map(id).toProperty("id")
                .map(firstName).toProperty("firstName")
                .map(lastName).toProperty("lastName")
                .map(occupation).toNull()
                .build()
                .render(RenderingStrategies.MYBATIS3);

        String expected = "insert into foo (id, first_name, last_name, occupation) "
                + "values (#{record.id,jdbcType=INTEGER}, #{record.firstName,jdbcType=VARCHAR}, #{record.lastName,jdbcType=VARCHAR}, null)";
        assertThat(insertStatement.getInsertStatement()).isEqualTo(expected);
    }

    @Test
    void testInsertStatementBuilderWithConstants() {

        TestRecord record = new TestRecord();
        
        InsertStatementProvider<TestRecord> insertStatement = insert(record)
                .into(foo)
                .map(id).toConstant("3")
                .map(firstName).toProperty("firstName")
                .map(lastName).toProperty("lastName")
                .map(occupation).toStringConstant("Y")
                .build()
                .render(RenderingStrategies.MYBATIS3);

        String expected = "insert into foo (id, first_name, last_name, occupation) "
                + "values (3, #{record.firstName,jdbcType=VARCHAR}, #{record.lastName,jdbcType=VARCHAR}, 'Y')";
        assertThat(insertStatement.getInsertStatement()).isEqualTo(expected);
    }
    
    @Test
    void testSelectiveInsertStatementBuilder() {
        TestRecord record = new TestRecord();
        record.setLastName("jones");
        record.setOccupation("dino driver");
        
        InsertStatementProvider<TestRecord> insertStatement = insert(record)
                .into(foo)
                .map(id).toPropertyWhenPresent("id", record::getId)
                .map(firstName).toPropertyWhenPresent("firstName", record::getFirstName)
                .map(lastName).toPropertyWhenPresent("lastName", record::getLastName)
                .map(occupation).toPropertyWhenPresent("occupation", record::getOccupation)
                .build()
                .render(RenderingStrategies.MYBATIS3);

        String expected = "insert into foo (last_name, occupation) "
                + "values (#{record.lastName,jdbcType=VARCHAR}, #{record.occupation,jdbcType=VARCHAR})";
        assertThat(insertStatement.getInsertStatement()).isEqualTo(expected);
    }

    @Test
    void testParallelStream() {

        List<FieldAndValue> mappings = new ArrayList<>();
        
        mappings.add(newFieldAndValue(id.name(), "{record.id}"));
        mappings.add(newFieldAndValue(firstName.name(), "{record.firstName}"));
        mappings.add(newFieldAndValue(lastName.name(), "{record.lastName}"));
        mappings.add(newFieldAndValue(occupation.name(), "{record.occupation}"));
        
        FieldAndValueCollector collector = 
                mappings.parallelStream().collect(Collector.of(
                        FieldAndValueCollector::new,
                        FieldAndValueCollector::add,
                        FieldAndValueCollector::merge));
                
        String expectedColumnsPhrase = "(id, first_name, last_name, occupation)";
        String expectedValuesPhrase = "values ({record.id}, {record.firstName}, {record.lastName}, {record.occupation})";
        
        assertAll(
                () -> assertThat(collector.columnsPhrase()).isEqualTo(expectedColumnsPhrase),
                () -> assertThat(collector.valuesPhrase()).isEqualTo(expectedValuesPhrase)
        );
    }
    
    private FieldAndValue newFieldAndValue(String fieldName, String valuePhrase) {
        return FieldAndValue.withFieldName(fieldName)
                .withValuePhrase(valuePhrase)
                .build();
    }
    
    @Test
    void testParallelStreamWithParameters() {

        List<FieldAndValueAndParameters> mappings = new ArrayList<>();
        
        mappings.add(newFieldAndValueAndParameter(id.name(), "{p1}", "p1", 1));
        mappings.add(newFieldAndValueAndParameter(firstName.name(), "{p2}", "p2", "Fred"));
        mappings.add(newFieldAndValueAndParameter(lastName.name(), "{p3}", "p3", "Flintstone"));
        mappings.add(newFieldAndValueAndParameter(occupation.name(), "{p4}", "p4", "Driver"));
        
        FieldAndValueAndParametersCollector collector = 
                mappings.parallelStream().collect(Collector.of(
                        FieldAndValueAndParametersCollector::new,
                        FieldAndValueAndParametersCollector::add,
                        FieldAndValueAndParametersCollector::merge));
                
        String expectedColumnsPhrase = "(id, first_name, last_name, occupation)";
        String expectedValuesPhrase = "values ({p1}, {p2}, {p3}, {p4})";
        
        assertAll(
                () -> assertThat(collector.columnsPhrase()).isEqualTo(expectedColumnsPhrase),
                () -> assertThat(collector.valuesPhrase()).isEqualTo(expectedValuesPhrase),
                () -> assertThat(collector.parameters()).hasSize(4)
        );
    }
    
    private FieldAndValueAndParameters newFieldAndValueAndParameter(String fieldName, String valuePhrase, String parameterName,
            Object parameterValue) {
        return FieldAndValueAndParameters.withFieldName(fieldName)
                .withValuePhrase(valuePhrase)
                .withParameter(parameterName, parameterValue)
                .build();
    }
    
    @Test
    void testParallelStreamForMultiRecord() {

        List<FieldAndValue> mappings = new ArrayList<>();
        
        mappings.add(newFieldAndValues(id.name(), "#{records[%s].id}"));
        mappings.add(newFieldAndValues(firstName.name(), "#{records[%s].firstName}"));
        mappings.add(newFieldAndValues(lastName.name(), "#{records[%s].lastName}"));
        mappings.add(newFieldAndValues(occupation.name(), "#{records[%s].occupation}"));
        
        FieldAndValueCollector collector = 
                mappings.parallelStream().collect(Collector.of(
                        FieldAndValueCollector::new,
                        FieldAndValueCollector::add,
                        FieldAndValueCollector::merge));
                
        String expectedColumnsPhrase = "(id, first_name, last_name, occupation)";
        String expectedValuesPhrase = "values"
                + " (#{records[0].id}, #{records[0].firstName}, #{records[0].lastName}, #{records[0].occupation}),"
                + " (#{records[1].id}, #{records[1].firstName}, #{records[1].lastName}, #{records[1].occupation})";
        
        assertAll(
                () -> assertThat(collector.columnsPhrase()).isEqualTo(expectedColumnsPhrase),
                () -> assertThat(collector.multiRowInsertValuesPhrase(2)).isEqualTo(expectedValuesPhrase)
        );
    }
    
    private FieldAndValue newFieldAndValues(String fieldName, String valuePhrase) {
        return FieldAndValue.withFieldName(fieldName)
                .withValuePhrase(valuePhrase)
                .build();
    }
    
    static class TestRecord {
        private Integer id;
        private String firstName;
        private String lastName;
        private String occupation;

        Integer getId() {
            return id;
        }

        void setId(Integer id) {
            this.id = id;
        }

        String getFirstName() {
            return firstName;
        }

        void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        String getLastName() {
            return lastName;
        }

        void setLastName(String lastName) {
            this.lastName = lastName;
        }

        String getOccupation() {
            return occupation;
        }

        void setOccupation(String occupation) {
            this.occupation = occupation;
        }
    }
}
