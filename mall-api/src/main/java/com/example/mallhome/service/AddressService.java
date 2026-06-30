package com.example.mallhome.service;

import com.example.mallhome.entity.Address;
import com.example.mallhome.repository.AddressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserAddress(Long userId, Long id) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在"));
        return toMap(address);
    }

    @Transactional
    public Map<String, Object> addAddress(Long userId, Map<String, Object> payload) {
        String receiverName = required(payload, "receiverName", "请输入收件人姓名");
        String phone = required(payload, "phone", "请输入手机号");
        String province = required(payload, "province", "请选择省份");
        String city = required(payload, "city", "请选择城市");
        String district = required(payload, "district", "请选择区县");
        String detailAddress = required(payload, "detailAddress", "请输入详细地址");

        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName(receiverName);
        address.setPhone(phone);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setDetailAddress(detailAddress);

        long count = addressRepository.countByUserId(userId);
        address.setDefault(count == 0);

        address = addressRepository.save(address);
        return toMap(address);
    }

    @Transactional
    public Map<String, Object> updateAddress(Long userId, Long id, Map<String, Object> payload) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在"));

        if (payload.containsKey("receiverName")) address.setReceiverName(required(payload, "receiverName", "请输入收件人姓名"));
        if (payload.containsKey("phone")) address.setPhone(required(payload, "phone", "请输入手机号"));
        if (payload.containsKey("province")) address.setProvince(required(payload, "province", "请选择省份"));
        if (payload.containsKey("city")) address.setCity(required(payload, "city", "请选择城市"));
        if (payload.containsKey("district")) address.setDistrict(required(payload, "district", "请选择区县"));
        if (payload.containsKey("detailAddress")) address.setDetailAddress(required(payload, "detailAddress", "请输入详细地址"));

        address = addressRepository.save(address);
        return toMap(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long id) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在"));
        addressRepository.delete(address);
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在"));

        // 取消其他默认
        List<Address> all = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        for (Address a : all) {
            if (a.isDefault()) {
                a.setDefault(false);
                addressRepository.save(a);
            }
        }

        address.setDefault(true);
        addressRepository.save(address);
    }

    private Map<String, Object> toMap(Address address) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", address.getId());
        map.put("receiverName", address.getReceiverName());
        map.put("phone", address.getPhone());
        map.put("province", address.getProvince());
        map.put("city", address.getCity());
        map.put("district", address.getDistrict());
        map.put("detailAddress", address.getDetailAddress());
        map.put("isDefault", address.isDefault());
        map.put("createdAt", address.getCreatedAt());
        return map;
    }

    private String required(Map<String, Object> payload, String key, String message) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return String.valueOf(value).trim();
    }
}
