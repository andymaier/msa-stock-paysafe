package com.predic8.stock.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predic8.stock.model.Basket;
import com.predic8.stock.model.Stock;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopListener {
	private final ObjectMapper mapper;
	private final Map<String, Stock> stocks;
	private final NullAwareBeanUtilsBean beanUtils;
	private final KafkaTemplate kafka;

	public ShopListener(ObjectMapper mapper, Map<String, Stock> stocks, NullAwareBeanUtilsBean beanUtils, KafkaTemplate<String, Operation> kafka) {
		this.mapper = mapper;
		this.stocks = stocks;
		this.beanUtils = beanUtils;
		this.kafka = kafka;
	}

	@KafkaListener(topics = "shop")
	public void listen(Operation op) throws Exception {
		System.out.println("op = " + op);

		Stock stock = mapper.treeToValue(op.getObject(), Stock.class);
		switch (op.getBo()) {
			case "article":
				handleArticle(op, stock);
				break;
			case "basket":
				handleBasket(mapper.convertValue(op.getObject(), Basket.class));
			case "stock":
				handleStock(op, stock);
				break;
		}
	}

	private void handleStock(Operation op, Stock stock) {
		switch (op.getAction()) {
			case "create":
			case "upsert":
				stocks.put(stock.getUuid(), stock);
				break;
		}
	}

	private void handleArticle(Operation op, Stock stock) throws InvocationTargetException, IllegalAccessException {
		switch (op.getAction()) {
			case "create":
			case "upsert":
				Stock orig = stocks.get(stock.getUuid());
				if (orig != null) {
					beanUtils.copyProperties(stock, orig);
					stocks.put(orig.getUuid(), orig);
				} else {
					stocks.put(stock.getUuid(), stock);
				}
				break;
			case "delete":
				stocks.remove(stock.getUuid());
				break;
		}
	}

	private void handleBasket(Basket basket) {
		basket
				.getItems()
				.stream()
				.map(item ->
						new Operation("stock", "upsert", mapper.valueToTree(
								new Stock(item.getArticleId(), stocks.get(item.getArticleId()).getQuantity() - item.getQuantity())))
				)
				.forEach(op -> {
					try {
						op.logSend();

						kafka.send("shop", op).get(100, TimeUnit.MILLISECONDS);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}
}