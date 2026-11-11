package org.jeecg.modules.pcms.energy.vo;

import lombok.Data;

@Data
public  class DayStatistics {
        private final double sumP83022;      // 当日p83022总和
        private final double averageP83025;  // 当日p83025平均值
        private final int dataCount;         // 当日数据条数

        private final  double revenue;   //收益
        
        public DayStatistics(double sumP83022, double averageP83025, int dataCount,double revenue) {
            this.sumP83022 = sumP83022;
            this.averageP83025 = averageP83025;
            this.dataCount = dataCount;
            this.revenue = revenue;
        }
        
        // getter方法
        public double getSumP83022() { return sumP83022; }
        public double getAverageP83025() { return averageP83025; }
        public int getDataCount() { return dataCount; }

        public double getDataRevenue() { return revenue; }
        
        @Override
        public String toString() {
            return String.format("p83022总和=%.2f, p83025平均值=%.2f, 数据量=%d", 
                    sumP83022, averageP83025, dataCount);
        }
    }