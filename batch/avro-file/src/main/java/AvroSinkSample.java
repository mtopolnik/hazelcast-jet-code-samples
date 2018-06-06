/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.hazelcast.jet.IMapJet;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.avro.AvroSinks;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sources;
import model.User;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


/**
 * Demonstrates dumping a map's values to an Apache Avro file.
 */
public class AvroSinkSample {

    public static final String MAP_NAME = "userMap";
    public static final String DIRECTORY_NAME;

    static {
        Path path = Paths.get(AvroSinkSample.class.getClassLoader().getResource("").getPath());
        DIRECTORY_NAME = path.getParent().getParent().toString() + "/users";
    }

    private JetInstance jet;

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "log4j");
        new AvroSinkSample().go();
    }

    private void go() throws Exception {
        try {
            setup();
            jet.newJob(buildPipeline()).join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private void setup() {
        jet = Jet.newJetInstance();
        Jet.newJetInstance();

        IMapJet<String, User> map = jet.getMap(MAP_NAME);
        for (int i = 0; i < 100; i++) {
            User user = new User("User" + i, "pass" + i, i, i % 2 == 0);
            map.put(user.getUsername(), user);
        }
    }

    private static Pipeline buildPipeline() {
        Pipeline p = Pipeline.create();

        p.drawFrom(Sources.<String, User>map(MAP_NAME))
         .map(Map.Entry::getValue)
         .drainTo(AvroSinks.files(DIRECTORY_NAME, AvroSinkSample::schemaForUser, User.class));

        return p;
    }

    private static Schema schemaForUser() {
        return SchemaBuilder.record(User.class.getSimpleName())
                            .namespace(User.class.getPackage().getName())
                            .fields()
                                .name("username").type().stringType().noDefault()
                                .name("password").type().stringType().noDefault()
                                .name("age").type().intType().noDefault()
                                .name("status").type().booleanType().noDefault()
                            .endRecord();
    }
}
