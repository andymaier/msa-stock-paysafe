package com.predic8.stock.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predic8.stock.error.NotFoundException;
import com.predic8.stock.model.Stock;
import com.predic8.stock.event.Operation;
import java.util.Collection;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/stocks")
@RestController
public class StockRestController {
	private final Map<String, Stock> stocks;

	private ObjectMapper mapper;

	private KafkaTemplate<String, Operation> kafka;

	public StockRestController(Map<String, Stock> articles, ObjectMapper mapper, KafkaTemplate<String, Operation> kafka) {
		this.stocks = articles;
		this.mapper = mapper;
		this.kafka = kafka;
	}

	@GetMapping
	public Collection<Stock> index() {
		return stocks.values();
	}

	@GetMapping("/count")
	public long count() {
		return stocks.size();
	}

	@GetMapping("/{id}")
	public Stock get(@PathVariable String id) {
		Stock stock = stocks.get(id);
		if(stock == null) throw new NotFoundException();
		return stock;
	}

	@PutMapping("/{id}/{quantity}")
	public void setStock(@PathVariable String id, @PathVariable Integer quantity) {
		Stock stock = get(id);
		stock.setQuantity(quantity);
		Operation op = new Operation("stock", "upsert", mapper.valueToTree(stock));
		kafka.send(new ProducerRecord<>("shop", op));
	}
}