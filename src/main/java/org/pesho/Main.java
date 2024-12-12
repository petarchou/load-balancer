import org.pesho.config.RedisClient;
import org.pesho.servers.JettyServer;

static void main() {
    RedisClient.connect();
    new JettyServer().start();
}
