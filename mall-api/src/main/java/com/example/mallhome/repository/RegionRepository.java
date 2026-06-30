package com.example.mallhome.repository;

import com.example.mallhome.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, String> {

    List<Region> findByParentCodeOrderByCodeAsc(String parentCode);
}
