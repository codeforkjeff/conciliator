package com.codefork.refine.controllers;

import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.datasource.stats.Interval;
import com.codefork.refine.resources.StatsDataSource;
import com.codefork.refine.resources.StatsReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stats")
public class StatsController {

    List<DataSource> dataSources;

    @Autowired
    public StatsController(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    @RequestMapping(value = "")
    @ResponseBody
    public StatsReport stats() {
        List<StatsDataSource> statsDataSources = dataSources.stream().map(ds -> {
            return ds.getStats().generateReport();
        }).collect(Collectors.toList());

        StatsReport statsReport = new StatsReport();
        statsReport.setTimestamp(Interval.timestamp());
        statsReport.setDate(ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        statsReport.setDataSources(statsDataSources);

        return statsReport;
    }
}
