package com.example.vivek.mqttconnection;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import org.eclipse.kapua.gateway.client.Application;
import org.eclipse.kapua.gateway.client.Client;
import org.eclipse.kapua.gateway.client.MessageHandler;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Sender;
import org.eclipse.kapua.gateway.client.Topic;
import org.eclipse.kapua.gateway.client.mqtt.fuse.FuseClient;
import org.eclipse.kapua.gateway.client.profile.kura.KuraMqttProfile;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

import static org.eclipse.kapua.gateway.client.Credentials.userAndPassword;
import static org.eclipse.kapua.gateway.client.Errors.ignore;
import static org.eclipse.kapua.gateway.client.Transport.waitForConnection;

public class MainActivity extends AppCompatActivity {


    MqttAndroidClient client;
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Connect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    public void Connect() throws Exception
    {
        try (Client client = KuraMqttProfile.newProfile(FuseClient.Builder::new)
                .accountName("kapua-sys")
                .clientId("foo-bar")
                .brokerUrl("tcp://192.168.1.200:1883")
                .credentials(userAndPassword("kapua-broker", "kapua-password"))
                .build()) {

            Application.Builder builder=client.buildApplication("app");
            try {
                final Application application = builder.build();
                // wait for connection

                waitForConnection(application.transport());

                // subscribe to a topic

                application.data(Topic.of("my", "topic")).subscribe(message -> {
                    System.out.format("Received: %s%n", message);
                });

                // example payload

                final Payload.Builder payload = new Payload.Builder();
                payload.put("foo", "bar");
                payload.put("a", 1);

                try {
                    // send, handling error ourself
                    application.data(Topic.of("my", "topic")).send(payload);
                } catch (final Exception e) {
                    logger.info("Failed to publish", e);
                }

                // send, with attached error handler

                application.data(Topic.of("my", "receiver")).subscribe(new MessageHandler() {
                    @Override
                    public void handleMessage(Payload payload) {
                        System.out.format("Received: %s%n",payload );
                    }
                });

                // ignoring error

                application.data(Topic.of("my", "topic")).errors(ignore()).send(payload);

                // cache sender instance

                final Sender<RuntimeException> sender = application.data(Topic.of("my", "topic")).errors(ignore());

                int i = 0;
                while (i < 10) {
                    // send
                    sender.send(Payload.of("counter", i++));
                    Thread.sleep(1_000);
                }

                // sleep to not run into Paho thread starvation
                // Thread.sleep(100_000);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                e.getMessage();
            }

            Thread.sleep(1_000);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            e.getMessage();
        }
    }
}