package com.example.mallhome.controller.frontend;

import com.example.mallhome.entity.Region;
import com.example.mallhome.repository.RegionRepository;
import com.example.mallhome.service.AddressService;
import com.example.mallhome.service.CustomerAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AddressController {

    private final AddressService addressService;
    private final CustomerAuthService customerAuthService;
    private final RegionRepository regionRepository;

    public AddressController(AddressService addressService, CustomerAuthService customerAuthService,
                             RegionRepository regionRepository) {
        this.addressService = addressService;
        this.customerAuthService = customerAuthService;
        this.regionRepository = regionRepository;
    }

    @GetMapping("/api/mall/regions")
    public List<Map<String, String>> listRegions(@RequestParam(required = false) String parentCode) {
        String code = (parentCode == null || parentCode.isBlank()) ? "0" : parentCode;
        return regionRepository.findByParentCodeOrderByCodeAsc(code).stream().map(r -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("code", r.getCode());
            m.put("name", r.getName());
            m.put("level", String.valueOf(r.getLevel()));
            return m;
        }).toList();
    }

    @GetMapping("/api/mall/addresses")
    public List<Map<String, Object>> listAddresses(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return addressService.listAddresses(userId);
    }

    @PostMapping("/api/mall/addresses")
    public Map<String, Object> addAddress(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return addressService.addAddress(userId, payload);
    }

    @PutMapping("/api/mall/addresses/{id}")
    public Map<String, Object> updateAddress(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return addressService.updateAddress(userId, id, payload);
    }

    @DeleteMapping("/api/mall/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        addressService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/mall/addresses/{id}/default")
    public ResponseEntity<Void> setDefault(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        addressService.setDefault(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long requireUserId(HttpServletRequest request) {
        Map<String, Object> customer = customerAuthService.requireUser(extractToken(request));
        return Long.valueOf(String.valueOf(customer.get("id")));
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return "";
    }
}
