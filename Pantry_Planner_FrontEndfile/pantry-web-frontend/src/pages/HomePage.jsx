import { Link } from "react-router-dom";
import Logo from "../components/Logo";

const steps = [
  {
    number: "01",
    title: "Add your pantry",
    description: "Keep a quick list of the ingredients you already have.",
  },
  {
    number: "02",
    title: "Find recipe matches",
    description: "See what you can cook now and what is nearly within reach.",
  },
  {
    number: "03",
    title: "Shop only for gaps",
    description: "Turn missing ingredients into a focused shopping list.",
  },
];

export default function HomePage({ notice }) {
  return (
    <main className="overflow-hidden bg-slate-50 text-slate-900">
      <section className="relative">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-96 bg-gradient-to-b from-emerald-100/80 to-transparent" />
        <div className="relative mx-auto grid min-h-[calc(100dvh-4rem)] max-w-7xl items-center gap-14 px-4 py-16 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:px-8 lg:py-20">
          <div className="max-w-2xl">
            {notice && (
              <p
                className="mb-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
                role="alert"
              >
                {notice}
              </p>
            )}
            <p className="mb-5 inline-flex rounded-full border border-emerald-200 bg-white/80 px-4 py-2 text-sm font-semibold text-emerald-800 shadow-sm">
              Plan meals from what you already have
            </p>
            <h1 className="text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl lg:text-6xl">
              Turn your pantry into dinner.
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-8 text-slate-600 sm:text-xl">
              Pantry Planner finds recipes around your ingredients, helps you
              spot what is missing, and keeps your next grocery trip focused.
            </p>
            <div className="mt-9 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/signup"
                className="inline-flex min-h-12 items-center justify-center rounded-xl bg-emerald-700 px-6 font-semibold text-white shadow-lg shadow-emerald-200 transition hover:bg-emerald-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2"
              >
                Start planning free
              </Link>
              <Link
                to="/login"
                className="inline-flex min-h-12 items-center justify-center rounded-xl border border-slate-300 bg-white px-6 font-semibold text-slate-700 transition hover:border-emerald-300 hover:text-emerald-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2"
              >
                I already have an account
              </Link>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-xl">
            <div className="absolute -inset-6 rounded-[2.5rem] bg-emerald-200/50 blur-3xl" />
            <div className="relative rounded-3xl border border-emerald-100 bg-white p-5 shadow-2xl shadow-emerald-950/10 sm:p-7">
              <div className="flex items-center justify-between border-b border-slate-100 pb-5">
                <div>
                  <p className="text-sm font-medium text-slate-500">Tonight's match</p>
                  <h2 className="mt-1 text-xl font-bold text-slate-900">Creamy tomato pasta</h2>
                </div>
                <span className="rounded-full bg-emerald-100 px-3 py-1.5 text-sm font-bold text-emerald-800">
                  92% match
                </span>
              </div>

              <div className="my-6 flex items-center gap-4 rounded-2xl bg-emerald-50 p-4 sm:p-5">
                <Logo size={54} label="" />
                <div>
                  <p className="font-semibold text-slate-900">Ready in 25 minutes</p>
                  <p className="mt-1 text-sm text-slate-600">You already have 6 of 7 ingredients</p>
                </div>
              </div>

              <p className="text-sm font-semibold uppercase tracking-wider text-slate-500">
                In your pantry
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                {["Pasta", "Tomatoes", "Garlic", "Cream", "Basil", "Olive oil"].map(
                  (ingredient) => (
                    <span
                      key={ingredient}
                      className="rounded-full border border-emerald-200 bg-white px-3 py-1.5 text-sm font-medium text-emerald-800"
                    >
                      {ingredient}
                    </span>
                  )
                )}
              </div>

              <div className="mt-6 flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-amber-700">
                    One item missing
                  </p>
                  <p className="mt-0.5 font-semibold text-slate-800">Parmesan</p>
                </div>
                <span className="text-sm font-semibold text-amber-800">Add to list</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="border-t border-slate-200 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
          <div className="grid gap-5 md:grid-cols-3">
            {steps.map((step) => (
              <article
                key={step.number}
                className="rounded-2xl border border-slate-200 bg-slate-50 p-6"
              >
                <span className="text-sm font-bold text-emerald-700">{step.number}</span>
                <h2 className="mt-3 text-lg font-bold text-slate-900">{step.title}</h2>
                <p className="mt-2 leading-7 text-slate-600">{step.description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}
