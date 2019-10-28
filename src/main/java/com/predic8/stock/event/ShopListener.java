package com.predic8.stock.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predic8.stock.model.Stock;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ShopListener {
	private final ObjectMapper mapper;
	private final Map<String, Stock> repo;
	private final NullAwareBeanUtilsBean beanUtils;

	public ShopListener(ObjectMapper mapper, Map<String, Stock> repo, NullAwareBeanUtilsBean beanUtils) {
		this.mapper = mapper;
		this.repo = repo;
		this.beanUtils = beanUtils;
	}

	@KafkaListener(topics = "shop")
	public void listen(Operation op) throws Exception {
		System.out.println("op = " + op);

		Stock stock = mapper.treeToValue(op.getObject(), Stock.class);

		switch (op.getAction()) {
			case "upsert":
				repo.put(stock.getUuid(), stock);
				break;
			case "delete":
				repo.remove(stock.getUuid());
				break;
		}
	}
}