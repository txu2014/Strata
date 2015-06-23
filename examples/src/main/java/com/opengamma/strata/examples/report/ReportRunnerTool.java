/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.pricing.PricingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.MarketDataBuilder;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.function.OpenGammaPricingRules;
import com.opengamma.strata.report.Report;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.ReportRequirements;
import com.opengamma.strata.report.ReportRunner;
import com.opengamma.strata.report.ReportTemplate;
import com.opengamma.strata.report.cashflow.CashFlowReportRunner;
import com.opengamma.strata.report.cashflow.CashFlowReportTemplate;
import com.opengamma.strata.report.format.ReportOutputFormat;
import com.opengamma.strata.report.trade.TradeReportRunner;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Tool for running a report from the command line.
 */
public class ReportRunnerTool {
  
  @Parameter(names = {"-t", "--template"}, description = "Report template input file", required = true, converter = ReportTemplateParameterConverter.class)
  private ReportTemplate template;

  @Parameter(names = {"-m", "--marketdata"}, description = "Market data root directory", validateValueWith = MarketDataRootValidator.class)
  private File marketDataRoot;

  @Parameter(names = {"-p", "--portfolio"}, description = "Portfolio input file", required = true, converter = PortfolioParameterConverter.class)
  private TradePortfolio portfolio;

  @Parameter(names = {"-d", "--date"}, description = "Valuation date, YYYY-MM-DD", required = true, converter = LocalDateParameterConverter.class)
  private LocalDate valuationDate;

  @Parameter(names = {"-f", "--format"}, description = "Report output format, ascii or csv", converter = ReportOutputFormatParameterConverter.class)
  private ReportOutputFormat format = ReportOutputFormat.ASCII_TABLE;
  
  @Parameter(names = {"-i", "--id"}, description = "An ID by which to select a single trade")
  private String idSearch;

  @Parameter(names = {"-h", "--help"}, description = "Displays this message", help = true)
  private boolean help;

  /**
   * Runs the tool.
   * 
   * @param args  the command-line arguments
   */
  public static void main(String[] args) {
    ReportRunnerTool reportRunner = new ReportRunnerTool();
    JCommander commander = new JCommander(reportRunner);
    commander.setProgramName(ReportRunnerTool.class.getSimpleName());
    try {
      commander.parse(args);
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.println();
      commander.usage();
      return;
    }
    if (reportRunner.help) {
      commander.usage();
    } else {
      try {
        reportRunner.run();
      } catch (Exception e) {
        System.err.println(Messages.format("Error: {}\n", e.getMessage()));
        commander.usage();
      }
    }
  }

  //-------------------------------------------------------------------------
  private void run() {
    ReportRunner<ReportTemplate> reportRunner = getReportRunner(template);

    ReportRequirements requirements = reportRunner.requirements(template);
    ReportCalculationResults calculationResults = runCalculationRequirements(requirements);

    Report report = reportRunner.runReport(calculationResults, template);

    switch (format) {
      case ASCII_TABLE:
        report.writeAsciiTable(System.out);
        break;
      case CSV:
        report.writeCsv(System.out);
        break;
    }
  }

  private ReportCalculationResults runCalculationRequirements(ReportRequirements requirements) {
    List<Column> columns = requirements.getTradeMeasureRequirements();

    PricingRules pricingRules = OpenGammaPricingRules.standard();

    MarketDataBuilder marketDataBuilder = marketDataRoot == null ?
        ExampleMarketData.builder() : MarketDataBuilder.ofPath(marketDataRoot.toPath());

    CalculationRules rules = CalculationRules.builder()
        .pricingRules(pricingRules)
        .marketDataRules(marketDataBuilder.rules())
        .reportingRules(ReportingRules.empty())
        .build();

    BaseMarketData snapshot = marketDataBuilder.buildSnapshot(valuationDate);

    CalculationEngine calculationEngine = ExampleEngine.create();
    
    List<Trade> trades;
    if (StringUtils.isBlank(idSearch)) {
      trades = portfolio.getTrades();
    } else {
      trades = portfolio.getTrades().stream()
          .filter(t -> t.getTradeInfo().getId().isPresent() && t.getTradeInfo().getId().get().getValue().equals(idSearch))
          .collect(Collectors.toList());
      if (trades.size() > 1) {
        throw new IllegalArgumentException(
            Messages.format("More than one trade found matching ID: '{}'", idSearch));
      }
    }
    if (trades.isEmpty()) {
      throw new IllegalArgumentException("No trades found. Please check the input portfolio or trade ID filter.");
    }
    
    Results results = calculationEngine.calculate(trades, columns, rules, snapshot);
    return ReportCalculationResults.builder()
        .valuationDate(valuationDate)
        .trades(trades)
        .columns(requirements.getTradeMeasureRequirements())
        .calculationResults(results)
        .build();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ReportRunner<ReportTemplate> getReportRunner(ReportTemplate reportTemplate) {
    // double-casts to achieve result type, allowing report runner to be used without external knowledge of template type
    if (reportTemplate instanceof TradeReportTemplate) {
      return (ReportRunner) new TradeReportRunner();
    } else if (reportTemplate instanceof CashFlowReportTemplate) {
      return (ReportRunner) new CashFlowReportRunner();
    }
    throw new IllegalArgumentException(Messages.format("Unsupported report type: {}", reportTemplate.getClass().getSimpleName()));
  }

}
