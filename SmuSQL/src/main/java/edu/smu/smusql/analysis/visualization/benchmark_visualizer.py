import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns


class BenchmarkVisualizer:
    def __init__(self, csv_files):
        if isinstance(csv_files, str):
            csv_files = [csv_files]

        self.dfs = []
        for file in csv_files:
            df = pd.read_csv(file)
            engine_config = file.split('/')[-1].split('_results')[0]
            df['EngineConfig'] = engine_config
            self.dfs.append(df)

        self.df = pd.concat(self.dfs, ignore_index=True)
        self.df['Timestamp'] = pd.to_datetime(self.df['Timestamp'])

        # Create color palette
        self.engines = sorted(self.df['EngineConfig'].unique())
        self.color_palette = sns.color_palette("husl", len(self.engines))

        print(f"Loaded data from {len(csv_files)} files:")
        print(f"- Total rows: {len(self.df)}")
        print(f"- Engines: {', '.join(self.engines)}")
        print(f"- Query types: {', '.join(sorted(self.df['QueryType'].unique()))}")

        os.makedirs('results/images', exist_ok=True)

    def show_query_distribution(self):
        """Display query distribution as a DataFrame with counts and percentages."""
        query_counts = self.df.groupby(['EngineConfig', 'QueryType'])['QueryCount'].max().unstack()

        # Add total row and column
        query_counts.loc['Total'] = query_counts.sum()
        query_counts['Total'] = query_counts.sum(axis=1)

        # Calculate percentages
        percentages = query_counts.copy()
        for idx in percentages.index:
            if idx != 'Total':
                total = query_counts.loc[idx, 'Total']
                percentages.loc[idx] = (query_counts.loc[idx] / total * 100)

        # Format output
        formatted_df = pd.DataFrame()
        for col in query_counts.columns:
            formatted_df[col] = query_counts[col].map('{:,.0f}'.format) + \
                                ' (' + percentages[col].map('{:.1f}%'.format) + ')'

        return formatted_df

    def plot_execution_time_distribution(self):
        """Plot execution time distribution with log scale."""
        plt.figure(figsize=(15, 8))

        # Add a small epsilon to zero/very small values to prevent log(0)
        plot_data = self.df.copy()
        min_nonzero = plot_data['AverageExecutionTime'][plot_data['AverageExecutionTime'] > 0].min()
        epsilon = min_nonzero * 0.1  # Use 10% of smallest non-zero value
        plot_data.loc[plot_data['AverageExecutionTime'] <= min_nonzero, 'AverageExecutionTime'] = epsilon

        # Create boxplot
        sns.boxplot(data=plot_data,
                    x='QueryType',
                    y='AverageExecutionTime',
                    hue='EngineConfig',
                    palette=self.color_palette,
                    showfliers=False)

        plt.yscale('log')  # Set log scale for y-axis
        plt.xticks(rotation=45)
        plt.title('Execution Time Distribution by Query Type and Engine (Log Scale)')
        plt.ylabel('Average Execution Time (ms) - Log Scale')
        plt.xlabel('Query Type')
        plt.legend(title='Engine Configuration', bbox_to_anchor=(1.05, 1))

        # Add grid for both major and minor lines
        plt.grid(True, alpha=0.3, which='both')

        # Set reasonable y-axis limits
        plt.ylim(min_nonzero * 0.1, plot_data['AverageExecutionTime'].max() * 2)

        # Print summary statistics
        print("\nSummary Statistics by Query Type and Engine:")
        summary = plot_data.groupby(['QueryType', 'EngineConfig'])['AverageExecutionTime'].agg([
            'count', 'mean', 'std', 'min', 'max'
        ]).round(3)
        print(summary)

        plt.tight_layout()
        plt.savefig('results/images/execution_time_distribution.png', dpi=300)
        plt.show()

    def plot_success_rates(self):
        """Plot success rates by engine and query type."""
        success_rates = self.df.groupby(['EngineConfig', 'QueryType'])['SuccessRate'].mean().unstack()

        plt.figure(figsize=(12, 6))
        ax = success_rates.plot(kind='bar', width=0.8)
        plt.title('Success Rates by Engine Configuration and Query Type')
        plt.ylabel('Success Rate (%)')
        plt.legend(title='Query Type', bbox_to_anchor=(1.05, 1))
        plt.tick_params(axis='x', rotation=45)

        # Add value labels
        for container in ax.containers:
            ax.bar_label(container, fmt='%.1f%%', padding=3)

        plt.grid(True, alpha=0.3)
        plt.tight_layout()
        plt.savefig('results/images/success_rates.png', dpi=300)
        plt.show()

    def plot_performance_comparison(self):
        """Plot performance comparison metrics with simplified average execution time."""
        fig = plt.figure(figsize=(15, 12))

        # Create a grid for subplots - 2x2 grid
        gs = fig.add_gridspec(2, 2)

        # Average execution time by engine (simplified)
        ax1 = fig.add_subplot(gs[0, :])
        avg_time = self.df.groupby('EngineConfig')['AverageExecutionTime'].mean()
        avg_time.plot(kind='bar', ax=ax1, color=self.color_palette)
        ax1.set_title('Average Execution Time by Engine')
        ax1.set_xlabel('Engine Configuration')
        ax1.set_ylabel('Time (ms)')
        ax1.grid(True, alpha=0.3)
        ax1.tick_params(axis='x', rotation=45)

        # Add value labels
        ax1.bar_label(ax1.containers[0], fmt='%.2f', padding=3)

        # Total execution time by engine
        ax2 = fig.add_subplot(gs[1, 0])
        total_times = self.df.groupby(['EngineConfig'])['TotalExecutionTime'].sum()
        total_times.plot(kind='bar', ax=ax2)
        ax2.set_title('Total Execution Time by Engine')
        ax2.set_xlabel('Engine Configuration')
        ax2.set_ylabel('Total Time (ms)')
        ax2.grid(True, alpha=0.3)
        ax2.tick_params(axis='x', rotation=45)

        # Add value labels
        ax2.bar_label(ax2.containers[0], fmt='%.2f', padding=3)

        # Query distribution pie chart
        ax3 = fig.add_subplot(gs[1, 1])
        query_dist = self.df.groupby(['QueryType'])['QueryCount'].max()
        total_queries = query_dist.sum()
        query_dist_pct = (query_dist / total_queries) * 100

        colors = plt.cm.Set3(np.linspace(0, 1, len(query_dist)))
        wedges, texts, autotexts = ax3.pie(query_dist_pct,
                                           labels=query_dist.index,
                                           autopct='%1.1f%%',
                                           colors=colors,
                                           textprops={'fontsize': 9})

        # Rotate and format the label texts for better readability
        plt.setp(texts, rotation_mode="anchor", ha='center')
        plt.setp(autotexts, size=8, weight="bold")

        ax3.set_title('Query Type Distribution', pad=10)

        plt.tight_layout()

        # Save individual plots
        fig.savefig('results/images/performance_comparison_overview.png', dpi=300, bbox_inches='tight')

        # Save average execution time subplot with legend and title
        ax1.legend(title='Engine Configuration', bbox_to_anchor=(1.02, 1), loc='upper left')
        extent = ax1.get_window_extent().transformed(fig.dpi_scale_trans.inverted())
        fig.savefig('results/images/avg_execution_time_by_engine.png', bbox_inches=extent.expanded(1.6, 1.8), dpi=300)

        # Save total execution time subplot with legend and title
        ax2.legend(title='Engine Configuration', bbox_to_anchor=(1.02, 1), loc='upper left')
        extent = ax2.get_window_extent().transformed(fig.dpi_scale_trans.inverted())
        fig.savefig('results/images/total_execution_time_by_engine.png', bbox_inches=extent.expanded(1.8, 1.8), dpi=300)

        # Save query type distribution subplot with title
        extent = ax3.get_window_extent().transformed(fig.dpi_scale_trans.inverted())
        fig.savefig('results/images/query_type_distribution.png', bbox_inches=extent.expanded(1.4, 1.8), dpi=300)
        plt.show()

    def plot_query_time_trends(self):
        """Plot query time trends with linear scale."""
        query_types = sorted(self.df['QueryType'].unique())
        cols = 2
        rows = (len(query_types) + cols - 1) // cols

        fig, axes = plt.subplots(rows, cols, figsize=(15, 5 * rows))
        axes_flat = axes.flatten()

        for idx, query_type in enumerate(query_types):
            query_data = self.df[self.df['QueryType'] == query_type]
            ax = axes_flat[idx]

            # Calculate y-axis max for this query type to set consistent range
            max_time = query_data['AverageExecutionTime'].max()

            for engine in self.engines:
                engine_data = query_data[query_data['EngineConfig'] == engine]
                if not engine_data.empty:
                    ax.plot(engine_data['QueryCount'],
                            engine_data['AverageExecutionTime'],
                            label=engine,
                            marker='.',
                            alpha=0.7,
                            markersize=4)

            ax.set_title(f'{query_type} Performance Trends')
            ax.set_xlabel('Number of Queries')
            ax.set_ylabel('Average Execution Time (ms)')
            ax.grid(True, alpha=0.3)
            ax.legend(fontsize='small')
            ax.yaxis.set_major_formatter(plt.FormatStrFormatter('%.2f'))
            ax.set_ylim(-0.01, max_time * 1.1)

        # Remove empty subplots
        for idx in range(len(query_types), len(axes_flat)):
            fig.delaxes(axes_flat[idx])

        # Print summary statistics for each query type
        print("\nPerformance Trend Summary Statistics:")
        for query_type in query_types:
            print(f"\n{query_type}:")
            query_stats = self.df[self.df['QueryType'] == query_type].groupby('EngineConfig')[
                'AverageExecutionTime'].agg([
                'mean', 'std', 'min', 'max'
            ]).round(3)
            print(query_stats)

        plt.tight_layout()
        fig.savefig('results/images/query_time_trends_overview.png', dpi=300, bbox_inches='tight')
        plt.show()

    def plot_range_vs_equals_comparison(self):
        """Compare range vs equals operations."""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))

        select_data = self.df[self.df['QueryType'].isin(['RANGE_SELECT', 'EQUALS_SELECT'])]
        update_data = self.df[self.df['QueryType'].isin(['RANGE_UPDATE', 'EQUALS_UPDATE'])]

        # Plot SELECT operations
        sns.boxplot(data=select_data, x='EngineConfig', y='AverageExecutionTime',
                    hue='QueryType', ax=ax1)
        ax1.set_yscale('log')
        ax1.set_title('SELECT Operations: Range vs Equals')
        ax1.set_xticklabels(ax1.get_xticklabels(), rotation=45)
        ax1.grid(True, alpha=0.3)

        # Plot UPDATE operations
        sns.boxplot(data=update_data, x='EngineConfig', y='AverageExecutionTime',
                    hue='QueryType', ax=ax2)
        ax2.set_yscale('log')
        ax2.set_title('UPDATE Operations: Range vs Equals')
        ax2.set_xticklabels(ax2.get_xticklabels(), rotation=45)
        ax2.grid(True, alpha=0.3)

        plt.tight_layout()
        plt.savefig('results/images/range_vs_equals_comparison.png', dpi=300)
        plt.show()

    def plot_configuration_impact(self):
        """Compare performance between different configurations."""
        # Identify configurations (with/without cache)
        cache_configs = self.df['EngineConfig'].str.contains('withcache|nocache')
        if not cache_configs.any():
            print("No cache configurations found in the data")
            return

        nocache_data = self.df[self.df['EngineConfig'].str.contains('nocache')]
        withcache_data = self.df[self.df['EngineConfig'].str.contains('withcache')]

        if nocache_data.empty or withcache_data.empty:
            print("Missing data for cache comparison")
            return

        improvements = []
        for query_type in sorted(self.df['QueryType'].unique()):
            nocache_time = nocache_data[nocache_data['QueryType'] == query_type]['AverageExecutionTime'].mean()
            withcache_time = withcache_data[withcache_data['QueryType'] == query_type]['AverageExecutionTime'].mean()

            if nocache_time > 0:
                improvement = ((nocache_time - withcache_time) / nocache_time) * 100
                improvements.append({
                    'QueryType': query_type,
                    'Improvement': improvement
                })

        if improvements:
            improvement_df = pd.DataFrame(improvements)
            plt.figure(figsize=(12, 6))
            ax = sns.barplot(data=improvement_df, x='QueryType', y='Improvement')
            plt.title('Performance Impact of Caching')
            plt.ylabel('Performance Improvement (%)')
            plt.xticks(rotation=45)
            plt.axhline(y=0, color='black', linestyle='-', alpha=0.2)
            plt.grid(True, alpha=0.3)

            # Add value labels
            for i in ax.containers:
                ax.bar_label(i, fmt='%.1f%%')

            plt.tight_layout()
            plt.savefig('results/images/configuration_impact.png', dpi=300)
            plt.show()


if __name__ == "__main__":
    # Example usage
    visualizer = BenchmarkVisualizer([
        'results/bplusarray_nocache_results.csv',
        'results/bplusarray_withcache_results.csv'
    ])

    # Display query distribution
    print("\nQuery Distribution:")
    print(visualizer.show_query_distribution())

    # Generate all plots
    visualizer.plot_execution_time_distribution()
    visualizer.plot_success_rates()
    visualizer.plot_performance_comparison()
    visualizer.plot_query_time_trends()
    visualizer.plot_range_vs_equals_comparison()
    visualizer.plot_configuration_impact()
