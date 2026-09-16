
use clap::Parser;
use rdkafka::consumer::{Consumer, StreamConsumer};
use rdkafka::Message;
use rdkafka::config::ClientConfig;


#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    #[arg(long)]
    sasl: bool,
}

#[tokio::main]
async fn main() {

    let args = Args::parse();

    let mut config = ClientConfig::new();
    config.set("bootstrap.servers", "localhost:9092");
    config.set("client.id", "rust_consumer");
    if args.sasl {
        config.set("security.protocol", "SASL_PLAINTEXT");
        config.set("sasl.mechanisms", "SCRAM-SHA-512");
        config.set("sasl.username", "rust-producer");
        config.set("sasl.password", "rust-producer-password");
    }
    config.set("group.id", "example_group");
    config.set("enable.partition.eof", "false");
    config.set("auto.offset.reset", "earliest");

    let consumer: StreamConsumer = config.create().expect("Consumer creation failed");

    let topic = "my-topic";

    consumer
        .subscribe(&[topic])
        .expect("Failed to subscribe to topic");

    println!("Starting to consume messages...");
    loop {
        match consumer.recv().await {
            Ok(message) => {
                let payload = message.payload().unwrap_or(&[]);
                let key = message.key().unwrap_or(&[]);
                println!(
                    "Received message: key = {:?}, payload = {:?}, topic = {}, partition = {}, offset = {}",
                    String::from_utf8_lossy(key),
                    String::from_utf8_lossy(payload),
                    message.topic(),
                    message.partition(),
                    message.offset()
                );
            }
            Err(e) => eprintln!("Kafka error: {}", e),
        }
    }
}
