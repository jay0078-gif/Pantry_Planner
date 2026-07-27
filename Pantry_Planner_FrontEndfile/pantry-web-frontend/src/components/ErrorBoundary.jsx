import React from "react";

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    // Log to console; backend/error trackers could be added here
    console.error("ErrorBoundary caught:", error, info);
  }

  render() {
    const { error } = this.state;
    if (error) {
      return (
        <div className="max-w-6xl mx-auto p-4">
          <div className="bg-red-50 border border-red-200 text-red-800 p-4 rounded">
            <h2 className="font-semibold text-lg">Something went wrong</h2>
            <pre className="mt-2 text-sm whitespace-pre-wrap">{String(error && (error.message || error))}</pre>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
