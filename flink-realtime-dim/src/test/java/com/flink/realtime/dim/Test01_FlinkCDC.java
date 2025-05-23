package com.flink.realtime.dim;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @version 1.0
 * @ Package com.flink.realtime.dim.Test01_FlinkCDC
 * @ Author liu.bo
 * @ Date 2025/5/3 14:32
 * @ description: 演示FlinkCDC的使用
 */
public class Test01_FlinkCDC {
    public static void main(String[] args) throws Exception {
        //TODO 1.基本环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //TODO 2.设置并行度
        env.setParallelism(1);
        // enable checkpoint
//        env.enableCheckpointing(3000);
        //TODO 3.使用FlinkCDC读取MySQL表中的数据
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("192.168.217.134")
                .port(3306)
                .databaseList("gmall") // set captured database
                .tableList("gmall.order_info") // set captured table
                .username("root")
                .password("root")
                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
                .startupOptions(StartupOptions.earliest())
                .includeSchemaChanges(true)
                .build();

        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("192.168.217.134:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("topic_db")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .build();
        DataStreamSource<String> mySQL_source = env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");
        //mySQL_source.sinkTo(sink);
        mySQL_source.print();
        env.execute("Print MySQL Snapshot + Binlog");
    }
}
