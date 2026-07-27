// import { useEffect, useState } from "react";
// import { toast } from "react-toastify";
// import api from "../api";

// export default function ReviewRecipesPage() {
//   const [pending, setPending] = useState([]);
//   const [loading, setLoading] = useState(false);

//   /** 🔹 Load all pending (unapproved) recipe submissions */
//   async function loadPending() {
//     setLoading(true);
//     try {
//       // 🔧 fixed: removed extra `/api`
//       const res = await api.get("/admin/recipes/pending");
//       setPending(Array.isArray(res.data) ? res.data : []);
//     } catch (err) {
//       console.error("❌ Load pending failed:", err.response || err);
//       toast.error(
//         err.response?.data?.error ||
//           "Access denied or not logged in as admin/owner."
//       );
//     } finally {
//       setLoading(false);
//     }
//   }

//   /** 🔹 Approve a selected recipe */
//   async function approve(id) {
//     if (!window.confirm("Approve this recipe?")) return;
//     try {
//       // 🔧 fixed: removed extra `/api`
//       const res = await api.patch(`/admin/recipes/${id}/approve`);
//       const message =
//         res.data?.message ||
//         res.data?.error ||
//         "✅ Recipe approved and published!";
//       if (res.status === 200) toast.success(message);
//       else toast.info(message);
//       await loadPending();
//     } catch (err) {
//       console.error("❌ Approval failed:", err.response || err);
//       const msg =
//         err.response?.data?.error ||
//         err.response?.data?.message ||
//         "❌ Approval failed.";
//       toast.error(msg);
//     }
//   }

//   useEffect(() => {
//     loadPending();
//   }, []);

//   // -------------------------------------------------------------
//   // 🖼️ Render UI
//   // -------------------------------------------------------------
//   return (
//     <div className="container" style={{ padding: "2rem" }}>
//       <div
//         style={{
//           display: "flex",
//           justifyContent: "space-between",
//           alignItems: "center",
//           marginBottom: "1rem",
//         }}
//       >
//         <h2 style={{ margin: 0 }}>Pending Recipes for Approval</h2>
//         <button
//           disabled={loading}
//           onClick={loadPending}
//           style={{
//             backgroundColor: "#1976d2",
//             color: "white",
//             border: "none",
//             borderRadius: "4px",
//             padding: "0.4rem 0.8rem",
//             cursor: loading ? "wait" : "pointer",
//           }}
//         >
//           {loading ? "Loading…" : "🔄 Refresh"}
//         </button>
//       </div>

//       {loading && <p>⏳ Loading pending recipes…</p>}
//       {!loading && pending.length === 0 && (
//         <p style={{ color: "#666" }}>No pending submissions.</p>
//       )}

//       <ul style={{ listStyle: "none", paddingLeft: 0 }}>
//         {!loading &&
//           pending.map((r) => (
//             <li
//               key={r.id}
//               style={{
//                 border: "1px solid #ccc",
//                 borderRadius: "8px",
//                 padding: "1rem",
//                 marginBottom: "1.5rem",
//                 backgroundColor: "#fafafa",
//               }}
//             >
//               <h4 style={{ marginTop: 0 }}>{r.title}</h4>

//               <p style={{ whiteSpace: "pre-line" }}>
//                 {r.description || r.instructions}
//               </p>

//               {r.ingredients && (
//                 <p>
//                   <strong>Ingredients:</strong>{" "}
//                   {Array.isArray(r.ingredients)
//                     ? r.ingredients.join(", ")
//                     : r.ingredients}
//                 </p>
//               )}

//               <button
//                 onClick={() => approve(r.id)}
//                 style={{
//                   backgroundColor: "#4caf50",
//                   color: "white",
//                   border: "none",
//                   borderRadius: "4px",
//                   padding: "0.5rem 1rem",
//                   cursor: "pointer",
//                   fontWeight: 500,
//                 }}
//               >
//                 ✅ Approve
//               </button>
//             </li>
//           ))}
//       </ul>
//     </div>
//   );
// }

import { useEffect, useState } from "react";
import { toast } from "react-toastify";
import api from "../api";

export default function ReviewRecipesPage() {
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(false);

  async function loadPending() {
    setLoading(true);
    try {
      const res = await api.get("/admin/recipes/pending");
      setPending(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error("❌ Load pending failed:", err.response || err);
      toast.error(
        err.response?.data?.error ||
          "Access denied or not logged in as admin/owner."
      );
    } finally {
      setLoading(false);
    }
  }

  async function approve(id) {
    if (!window.confirm("Approve this recipe?")) return;
    try {
      const res = await api.patch(`/admin/recipes/${id}/approve`);
      const message =
        res.data?.message ||
        res.data?.error ||
        "✅ Recipe approved and published!";
      res.status === 200 ? toast.success(message) : toast.info(message);
      await loadPending();
    } catch (err) {
      console.error("❌ Approval failed:", err.response || err);
      const msg =
        err.response?.data?.error ||
        err.response?.data?.message ||
        "❌ Approval failed.";
      toast.error(msg);
    }
  }

  async function reject(id) {
    if (!window.confirm("Reject this recipe?")) return;
    try {
      const res = await api.patch(`/admin/recipes/${id}/reject`);
      const message =
        res.data?.message ||
        res.data?.error ||
        "❌ Recipe rejected successfully.";
      res.status === 200 ? toast.success(message) : toast.info(message);
      await loadPending();
    } catch (err) {
      console.error("❌ Rejection failed:", err.response || err);
      const msg =
        err.response?.data?.error ||
        err.response?.data?.message ||
        "❌ Rejection failed.";
      toast.error(msg);
    }
  }

  useEffect(() => {
    loadPending();
  }, []);

  return (
    <div className="container" style={{ padding: "2rem" }}>
      {/* Header + Reload button */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1rem",
        }}
      >
        <h2 style={{ margin: 0 }}>Pending Recipes for Approval</h2>

        {/* BIG REFRESH BUTTON */}
        <button
          disabled={loading}
          onClick={loadPending}
          style={{
            backgroundColor: "#1976d2",
            color: "white",
            border: "none",
            borderRadius: "8px",
            padding: "0.7rem 1.4rem",
            fontSize: "1rem",
            cursor: loading ? "wait" : "pointer",
            fontWeight: "600",
          }}
        >
          {loading ? "Loading…" : "🔄 Refresh"}
        </button>
      </div>

      {loading && <p>⏳ Loading pending recipes…</p>}
      {!loading && pending.length === 0 && (
        <p style={{ color: "#666" }}>No pending submissions.</p>
      )}

      <ul style={{ listStyle: "none", paddingLeft: 0 }}>
        {!loading &&
          pending.map((r) => (
            <li
              key={r.id}
              style={{
                border: "1px solid #ccc",
                borderRadius: "10px",
                padding: "1.2rem",
                marginBottom: "1.5rem",
                backgroundColor: "#ffffff",
                boxShadow: "0 2px 6px rgba(0,0,0,0.08)",
              }}
            >
              <h3 style={{ marginTop: 0 }}>{r.title}</h3>

              <p style={{ whiteSpace: "pre-line" }}>
                {r.description || r.instructions}
              </p>

              {r.ingredients && (
                <p>
                  <strong>Ingredients:</strong>{" "}
                  {Array.isArray(r.ingredients)
                    ? r.ingredients.join(", ")
                    : r.ingredients}
                </p>
              )}

              {/* BIG APPROVE & REJECT BUTTONS */}
              <div style={{ display: "flex", gap: "0.8rem", marginTop: "1rem" }}>
                <button
                  onClick={() => approve(r.id)}
                  style={{
                    backgroundColor: "#4caf50",
                    color: "white",
                    border: "none",
                    borderRadius: "8px",
                    padding: "0.75rem 1.5rem",
                    cursor: "pointer",
                    fontSize: "1rem",
                    fontWeight: 600,
                  }}
                >
                  ✅ Approve
                </button>

                <button
                  onClick={() => reject(r.id)}
                  style={{
                    backgroundColor: "#e53935",
                    color: "white",
                    border: "none",
                    borderRadius: "8px",
                    padding: "0.75rem 1.5rem",
                    cursor: "pointer",
                    fontSize: "1rem",
                    fontWeight: 600,
                  }}
                >
                  ❌ Reject
                </button>
              </div>
            </li>
          ))}
      </ul>
    </div>
  );
}
