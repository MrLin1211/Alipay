package com.example.testpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotifyStore {

    private static final int MAX_RECORDS = 100;

    private final ObjectMapper objectMapper;
    private final Deque<Map<String, Object>> records = new ArrayDeque<>();

    public NotifyStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void add(String body, boolean verified) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("receivedAt", LocalDateTime.now());
        record.put("verified", verified);
        try {
            record.put("payload", objectMapper.readTree(body));
        } catch (Exception exception) {
            record.put("payload", body);
        }
        records.addFirst(record);
        while (records.size() > MAX_RECORDS) {
            records.removeLast();
        }
    }

    public synchronized List<Map<String, Object>> list() {
        return new ArrayList<>(records);
    }
}
