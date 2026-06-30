package com.example.mallhome.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "region",
       indexes = { @Index(columnList = "parent_code") })
public class Region {

    @Id
    @Column(name = "code", length = 12)
    private String code;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "parent_code", length = 12)
    private String parentCode;

    public Region() {}

    public Region(String code, String name, int level, String parentCode) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.parentCode = parentCode;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
}
