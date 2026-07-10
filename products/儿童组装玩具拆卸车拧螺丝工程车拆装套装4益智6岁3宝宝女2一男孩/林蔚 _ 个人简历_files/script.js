const backToTop = document.querySelector(".back-to-top");
const navLinks = Array.from(document.querySelectorAll(".site-nav a"));
const sections = navLinks
  .map((link) => document.querySelector(link.getAttribute("href")))
  .filter(Boolean);

const toggleBackToTop = () => {
  backToTop.classList.toggle("is-visible", window.scrollY > 520);
};

const setActiveNav = (id) => {
  navLinks.forEach((link) => {
    link.classList.toggle("is-active", link.getAttribute("href") === `#${id}`);
  });
};

window.addEventListener("scroll", toggleBackToTop, { passive: true });

backToTop.addEventListener("click", () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
});

const updateActiveNav = () => {
  if (window.scrollY < window.innerHeight * 0.45) {
    setActiveNav("top");
    return;
  }

  const bottomActivationOffset = Math.max(96, window.innerHeight * 0.12);
  const nearPageBottom =
    window.innerHeight + window.scrollY >=
    document.documentElement.scrollHeight - bottomActivationOffset;

  if (nearPageBottom && sections.length > 0) {
    setActiveNav(sections.at(-1).id);
    return;
  }

  const viewportMarker = window.scrollY + window.innerHeight * 0.38;
  const currentSection =
    sections
      .filter((section) => section.offsetTop <= viewportMarker)
      .at(-1) || sections[0];

  if (currentSection) {
    setActiveNav(currentSection.id);
  }
};

window.addEventListener("scroll", updateActiveNav, { passive: true });
window.addEventListener("resize", updateActiveNav);

if (window.location.hash) {
  setActiveNav(window.location.hash.slice(1));
} else {
  updateActiveNav();
}

toggleBackToTop();
updateActiveNav();
