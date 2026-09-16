
use clap::Parser;
use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord};
use std::time::Duration;
use futures_util::future::FutureExt;


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
    config.set("linger.ms", "10");
    config.set("client.id", "rust_producer");
    if args.sasl {
            config.set("security.protocol", "SASL_PLAINTEXT");
            config.set("sasl.mechanisms", "SCRAM-SHA-512");
            config.set("sasl.username", "rust-producer");
            config.set("sasl.password", "rust-producer-password");
    }

    let producer: FutureProducer = config.create().expect("Producer creation failed");

    let topic = "my-topic";

    for i in 0..10000 {
        let key = format!("key-{}", i);
        let payload = format!("Hello, Kafka! Message {}", i);

        producer
            .send(FutureRecord::to(topic).payload(&payload).key(&key), Duration::from_secs(0))
            .then(|result| async move {
                match result {
                    Ok((partition, offset)) => {
                        eprintln!("Message delivered to partition {} with offset {}", partition, offset);
                    }
                    Err((e, _)) => {
                        eprintln!("Failed to deliver message: {}", e);
                    }
                }
            })
            .await;
    }

}
