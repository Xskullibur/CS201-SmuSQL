import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
from IPython.display import display, HTML


class BenchmarkVisualizer:
    def __init__(self, csv_files):
        """
        Initialize the visualizer with one or more CSV files containing benchmark results.

        Args:
            csv_files: List of paths to CSV files or single path string
            Format expected: 'engine_configuration_results.csv'
        """
        if isinstance(csv_files, str):
            csv_files = [csv_files]

        # Read and combine all CSV files
        dfs = []
        for file in csv_files:
            df = pd.read_csv(file)

            # Extract engine name and configuration from filename
            base_name = os.path.basename(file)
            # Remove '_results.csv' and split by '_'
            name_parts = base_name.replace('_results.csv', '').split('_')

            if len(name_parts) < 2:
                raise ValueError(f"Invalid filename format for {file}. Expected: engine_configuration_results.csv")

            # Use the first part as engine name and the remaining parts as configuration
            engine = name_parts[0]
            config = '_'.join(name_parts[1:])

            df['Engine'] = engine
            df['Configuration'] = config

            # Update EngineType column to include configuration
            df['EngineType'] = f"{engine}_{config}"

            dfs.append(df)

        self.df = pd.concat(dfs, ignore_index=True)

        # Convert timestamp to datetime
        self.df['Timestamp'] = pd.to_datetime(self.df['Timestamp'])

        # Store unique engines and configurations for later use
        self.engines = sorted(self.df['Engine'].unique())
        self.configurations = sorted(self.df['Configuration'].unique())

        # Generate a color palette for consistent colors across plots
        self.n_colors = len(self.df['EngineType'].unique())
        self.color_palette = sns.color_palette("husl", self.n_colors)

    def show_summary_statistics(self):
        """Display summary statistics for each engine type and query type."""
        summary = self.df.groupby(['EngineType', 'QueryType']).agg({
            'AverageExecutionTime': ['mean', 'std', 'min', 'max'],
            'SuccessRate': 'mean',
            'HeapMemoryUsed': 'mean'
        }).round(2)

        display(HTML(summary.to_html()))

    def plot_execution_time_distribution(self):
        """Plot distribution of execution times for each query type in separate subplots using linear scale."""
        query_types = sorted(self.df['QueryType'].unique())
        num_query_types = len(query_types)

        # Calculate subplot layout (2 columns)
        num_cols = 3
        num_rows = (num_query_types + num_cols - 1) // num_cols

        # Create figure and subplots
        fig, axes = plt.subplots(num_rows, num_cols,
                                 figsize=(15, 5 * num_rows),
                                 squeeze=False)
        axes_flat = axes.flatten()

        # Store statistics for each query type
        stats_by_type = {}

        # Create boxplot for each query type
        for idx, query_type in enumerate(query_types):
            ax = axes_flat[idx]
            query_data = self.df[self.df['QueryType'] == query_type]

            # Create boxplot
            sns.boxplot(data=query_data,
                        x='EngineType',
                        y='AverageExecutionTime',
                        palette=self.color_palette,
                        showfliers=True,  # Show outliers to see full distribution
                        ax=ax)

            # Calculate plot specific statistics
            mean_exec_time = query_data['AverageExecutionTime'].mean()
            std_exec_time = query_data['AverageExecutionTime'].std()

            # Set y-axis limit to mean + 2*std to focus on main distribution
            # while still showing some outliers
            ax.set_ylim(0, mean_exec_time + 2 * std_exec_time)

            # Customize subplot
            ax.set_title(f'{query_type} Execution Time Distribution')
            ax.set_xlabel('Engine Type')
            ax.set_ylabel('Time (ms)')
            ax.tick_params(axis='x', rotation=45)

            # Add mean line
            ax.axhline(y=mean_exec_time, color='r', linestyle='--', alpha=0.3)

            # Calculate statistics for this query type
            stats = query_data.groupby('EngineType')['AverageExecutionTime'].agg([
                'count',
                'mean',
                'median',
                'std',
                'min',
                'max',
                lambda x: x.quantile(0.25),  # Q1
                lambda x: x.quantile(0.75),  # Q3
                lambda x: len(x[x > (x.mean() + 2 * x.std())]),  # Count of outliers
            ]).round(3)
            stats.columns = ['Count', 'Mean', 'Median', 'Std', 'Min', 'Max', 'Q1', 'Q3', 'Outliers']
            stats_by_type[query_type] = stats

            # Add mean value as text
            ax.text(0.02, 0.98, f'Mean: {mean_exec_time:.2f}ms\nStd: {std_exec_time:.2f}ms',
                    transform=ax.transAxes,
                    verticalalignment='top',
                    bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))

        # Remove any unused subplots
        for idx in range(num_query_types, len(axes_flat)):
            fig.delaxes(axes_flat[idx])

        plt.tight_layout()
        plt.show()

        # Display statistics for each query type
        for query_type, stats in stats_by_type.items():
            display(HTML(f"<h3>Statistical Summary for {query_type}</h3>"))
            display(HTML(stats.style
                         .background_gradient(subset=['Mean', 'Median', 'Std'])
                         .to_html()))

        return stats_by_type

    def plot_success_rates(self):
        """Plot success rates for different query types and engines."""
        success_rates = self.df.groupby(['EngineType', 'QueryType'])['SuccessRate'].mean().unstack()

        plt.figure(figsize=(12, 6))
        success_rates.plot(kind='bar', width=0.8)
        plt.title('Success Rates by Engine and Query Type')
        plt.ylabel('Success Rate (%)')
        plt.legend(title='Query Type', bbox_to_anchor=(1.05, 1))
        plt.tick_params(axis='x', rotation=45)
        plt.tight_layout()
        plt.show()

    def plot_memory_usage_over_time(self):
        """
        Plot memory delta trends for each query type, comparing all engines within each plot.
        Shows the average memory change per operation type over time.
        """
        query_types = sorted(self.df['QueryType'].unique())
        num_query_types = len(query_types)

        # Calculate subplot layout
        num_cols = 2
        num_rows = (num_query_types + num_cols - 1) // num_cols

        fig, axes = plt.subplots(num_rows, num_cols,
                                 figsize=(15, 5 * num_rows),
                                 squeeze=False)

        axes_flat = axes.flatten()

        # Create a color map for engines
        engine_types = sorted(self.df['EngineType'].unique())
        colors = sns.color_palette("husl", len(engine_types))
        engine_colors = dict(zip(engine_types, colors))

        # Plot for each query type
        for idx, query_type in enumerate(query_types):
            ax = axes_flat[idx]

            query_data = self.df[self.df['QueryType'] == query_type]

            for engine_type in engine_types:
                engine_data = query_data[query_data['EngineType'] == engine_type]
                if not engine_data.empty:
                    engine_data = engine_data.sort_values('QueryCount')

                    # Use HeapMemoryDelta instead of HeapMemoryUsed
                    memory_mb = engine_data['HeapMemoryDelta'] / 1e6

                    # Plot moving average for smoother visualization
                    window_size = 50  # Adjust as needed
                    memory_ma = memory_mb.rolling(window=window_size, min_periods=1).mean()

                    ax.plot(engine_data['QueryCount'],
                            memory_ma,
                            label=engine_type,
                            color=engine_colors[engine_type],
                            alpha=0.7)

                    # # Add scatter plot for actual values with low alpha
                    # ax.scatter(engine_data['QueryCount'],
                    #            memory_mb,
                    #            color=engine_colors[engine_type],
                    #            alpha=0.1,
                    #            s=10)

            ax.set_title(f'Memory Impact - {query_type}')
            ax.set_xlabel('Number of Queries')
            ax.set_ylabel('Memory Change per Operation (MB)')
            ax.grid(True, alpha=0.3)
            ax.legend(bbox_to_anchor=(1.05, 1), loc='upper left')

            # Add horizontal line at y=0 to show baseline
            ax.axhline(y=0, color='black', linestyle='--', alpha=0.3)

        # Remove any unused subplots
        for idx in range(num_query_types, len(axes_flat)):
            fig.delaxes(axes_flat[idx])

        plt.tight_layout()
        plt.show()

    def plot_performance_comparison(self):
        """Create comparative visualization with log scale where appropriate."""
        fig, axes = plt.subplots(2, 2, figsize=(20, 15))

        # 1. Average execution time by engine (log scale)
        avg_time = self.df.groupby('EngineType')['AverageExecutionTime'].mean()
        avg_time.plot(kind='bar', ax=axes[0, 0], color=self.color_palette)
        axes[0, 0].set_yscale('log')
        axes[0, 0].set_title('Average Execution Time by Engine (Log Scale)')
        axes[0, 0].set_ylabel('Time (ms) - Log Scale')
        axes[0, 0].tick_params(axis='x', rotation=45)

        # 2. Query type distribution
        query_counts = self.df['QueryType'].value_counts()
        query_counts.plot(kind='pie', ax=axes[0, 1], autopct='%1.1f%%')
        axes[0, 1].set_title('Query Type Distribution')

        # 3. Success rate trends
        for i, engine_type in enumerate(sorted(self.df['EngineType'].unique())):
            engine_data = self.df[self.df['EngineType'] == engine_type]
            axes[1, 0].plot(engine_data['QueryCount'],
                            engine_data['SuccessRate'],
                            label=engine_type,
                            color=self.color_palette[i])
        axes[1, 0].set_title('Success Rate Trends')
        axes[1, 0].set_xlabel('Number of Queries')
        axes[1, 0].set_ylabel('Success Rate (%)')
        axes[1, 0].legend(bbox_to_anchor=(1.05, 1))

        # 4. Memory usage patterns (log scale)
        sns.boxplot(data=self.df, x='EngineType', y='HeapMemoryUsed',
                    ax=axes[1, 1], palette=self.color_palette, showfliers=False)
        axes[1, 1].set_yscale('log')
        axes[1, 1].set_title('Memory Usage Patterns (Log Scale)')
        axes[1, 1].set_ylabel('Heap Memory Used (bytes) - Log Scale')
        axes[1, 1].tick_params(axis='x', rotation=45)

        plt.tight_layout()
        plt.show()

        # Generate performance statistics
        perf_stats = self.df.groupby('EngineType').agg({
            'AverageExecutionTime': ['mean', 'median', 'std'],
            'SuccessRate': 'mean',
            'HeapMemoryUsed': ['mean', 'median', 'std']
        }).round(3)

        display(HTML("<h3>Performance Statistics by Engine</h3>"))
        display(HTML(perf_stats.to_html()))

        return perf_stats

    def plot_query_time_trends(self):
        """
        Plot execution time trends for each query type, comparing all engines within each plot.
        Creates a subplot for each type of query operation (INSERT, SELECT, UPDATE, etc.)
        """
        query_types = sorted(self.df['QueryType'].unique())
        num_query_types = len(query_types)

        # Calculate subplot layout
        num_cols = 2
        num_rows = (num_query_types + num_cols - 1) // num_cols

        fig, axes = plt.subplots(num_rows, num_cols,
                                 figsize=(15, 5 * num_rows),
                                 squeeze=False)  # squeeze=False ensures axes is always 2D

        # Flatten axes for easier iteration
        axes_flat = axes.flatten()

        # Plot for each query type
        for idx, query_type in enumerate(query_types):
            ax = axes_flat[idx]

            # Get data for this query type
            query_data = self.df[self.df['QueryType'] == query_type]

            # Plot each engine's trend line
            for engine_type in sorted(self.df['EngineType'].unique()):
                engine_data = query_data[query_data['EngineType'] == engine_type]
                if not engine_data.empty:
                    # Sort by QueryCount to ensure proper line plot
                    engine_data = engine_data.sort_values('QueryCount')
                    ax.plot(engine_data['QueryCount'],
                            engine_data['AverageExecutionTime'],
                            label=engine_type,
                            alpha=0.7)

                    # Optionally add scatter points for actual data points
                    ax.scatter(engine_data['QueryCount'],
                               engine_data['AverageExecutionTime'],
                               alpha=0.3,
                               s=10)

            ax.set_title(f'{query_type} Performance Trends')
            ax.set_xlabel('Number of Queries')
            ax.set_ylabel('Average Execution Time (ms)')
            ax.grid(True, alpha=0.3)
            ax.legend(bbox_to_anchor=(1.05, 1), loc='upper left')

        # Remove any unused subplots
        for idx in range(num_query_types, len(axes_flat)):
            fig.delaxes(axes_flat[idx])

        plt.tight_layout()
        plt.show()

    def plot_range_vs_equals_comparison(self):
        """Compare range vs equals operations with log scale and statistics."""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))

        select_ops = self.df[self.df['QueryType'].str.contains('SELECT')]
        update_ops = self.df[self.df['QueryType'].str.contains('UPDATE')]

        # Plot SELECT operations with log scale
        sns.boxplot(data=select_ops,
                    x='EngineType',
                    y='AverageExecutionTime',
                    hue='QueryType',
                    palette=sns.color_palette("Set2", 2),
                    ax=ax1)
        ax1.set_yscale('log')
        ax1.set_title('SELECT Operations: Range vs Equals (Log Scale)')
        ax1.set_xticklabels(ax1.get_xticklabels(), rotation=45)
        ax1.legend(bbox_to_anchor=(1.05, 1))
        ax1.set_ylabel('Average Execution Time (ms) - Log Scale')
        ax1.set_xlabel('Engine Type')

        # Plot UPDATE operations with log scale
        sns.boxplot(data=update_ops,
                    x='EngineType',
                    y='AverageExecutionTime',
                    hue='QueryType',
                    palette=sns.color_palette("Set2", 2),
                    ax=ax2)
        ax2.set_yscale('log')
        ax2.set_title('UPDATE Operations: Range vs Equals (Log Scale)')
        ax2.set_xticklabels(ax2.get_xticklabels(), rotation=45)
        ax2.legend(bbox_to_anchor=(1.05, 1))
        ax2.set_ylabel('Average Execution Time (ms) - Log Scale')
        ax2.set_xlabel('Engine Type')

        plt.tight_layout()
        plt.show()

        # Generate statistical comparison
        stats_dfs = []
        for ops, op_type in [(select_ops, 'SELECT'), (update_ops, 'UPDATE')]:
            stats_df = ops.groupby(['EngineType', 'QueryType'])['AverageExecutionTime'].agg([
                'mean',
                'median',
                'std',
                'min',
                'max',
                lambda x: x.quantile(0.25),  # Q1
                lambda x: x.quantile(0.75),  # Q3
            ]).round(3)
            stats_df.columns = ['Mean', 'Median', 'Std', 'Min', 'Max', 'Q1', 'Q3']
            display(HTML(f"<h3>Statistical Summary for {op_type} Operations</h3>"))
            display(HTML(stats_df.to_html()))
            stats_dfs.append(stats_df)

        return stats_dfs

    def plot_configuration_impact(self):
        """Compare performance between different configurations of the same engine."""
        plt.figure(figsize=(15, 8))

        for engine in self.engines:
            engine_data = self.df[self.df['Engine'] == engine]
            configs = sorted(engine_data['Configuration'].unique())

            # Skip if only one configuration
            if len(configs) < 2:
                continue

            # Use first configuration as baseline
            base_config = configs[0]
            base_data = engine_data[engine_data['Configuration'] == base_config]

            improvements_data = []

            for config in configs[1:]:
                config_data = engine_data[engine_data['Configuration'] == config]

                for query_type in sorted(engine_data['QueryType'].unique()):
                    base_time = base_data[base_data['QueryType'] == query_type]['AverageExecutionTime'].mean()
                    config_time = config_data[config_data['QueryType'] == query_type]['AverageExecutionTime'].mean()

                    if base_time > 0:
                        improvement = ((base_time - config_time) / base_time) * 100
                        improvements_data.append({
                            'Engine': engine,
                            'Configuration': config,
                            'QueryType': query_type,
                            'Improvement': improvement
                        })

            if improvements_data:
                improvements_df = pd.DataFrame(improvements_data)
                pivot_data = improvements_df.pivot(index='QueryType',
                                                   columns='Configuration',
                                                   values='Improvement')

                ax = pivot_data.plot(kind='bar', width=0.8)
                plt.title(f'Performance Impact of Configurations for {engine}\nCompared to {base_config}')
                plt.ylabel('Performance Improvement (%)')
                plt.xlabel('Query Type')
                plt.axhline(y=0, color='black', linestyle='-', alpha=0.2)
                plt.grid(True, axis='y')
                plt.xticks(rotation=45)

                # Add value labels
                for container in ax.containers:
                    ax.bar_label(container, fmt='%.1f%%', padding=3)

        plt.tight_layout()
        plt.show()


if __name__ == "__main__":
    visualizer = BenchmarkVisualizer([
        'results/bplus_nocache_results.csv',
        'results/bplus_withcache_results.csv',
        'results/hashmap_default_results.csv',
        'results/skiphash_default_results.csv',
        'results/skipindexed_default_results.csv',
    ])

    visualizer.show_summary_statistics()
    visualizer.plot_execution_time_distribution()
    visualizer.plot_success_rates()
    visualizer.plot_memory_usage_over_time()
    visualizer.plot_performance_comparison()
    visualizer.plot_query_time_trends()
    visualizer.plot_range_vs_equals_comparison()
    visualizer.plot_configuration_impact()
