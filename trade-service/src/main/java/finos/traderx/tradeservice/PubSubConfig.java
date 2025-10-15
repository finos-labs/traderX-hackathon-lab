package finos.traderx.tradeservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import finos.traderx.messaging.Publisher;
import finos.traderx.messaging.socketio.SocketIOJSONPublisher;
import finos.traderx.tradeservice.model.TradeOrder;
// CDM import removed - using String for JSON

@Configuration
public class PubSubConfig {
    @Value("${trade.feed.address}")
    private String tradeFeedAddress;

    @Bean 
    public Publisher<TradeOrder> tradePublisher() {
        SocketIOJSONPublisher<TradeOrder> publisher = new SocketIOJSONPublisher<TradeOrder>(){};
        publisher.setTopic("/trades");
        publisher.setSocketAddress(tradeFeedAddress);
        return publisher;
    }
    
    @Bean 
    public Publisher<String> cdmTradePublisher() {
        SocketIOJSONPublisher<String> publisher = new SocketIOJSONPublisher<String>(){};
        publisher.setTopic("/trades/cdm");
        publisher.setSocketAddress(tradeFeedAddress);
        return publisher;
    }

}
