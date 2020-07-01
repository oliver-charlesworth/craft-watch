import React, { ReactNode } from "react";
import Head from "next/head";

interface Props {
  title: string;
  description: string;
  children?: ReactNode;
}

const Page: React.FC<Props> = (props) => (
  <>
    <Head>
      <meta charSet="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <meta name="description" content={props.description} />

      <title>{props.title}</title>

      <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png" />
      <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png" />
      <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png" />
      <link rel="manifest" href="/site.webmanifest" />
      <link rel="mask-icon" href="/safari-pinned-tab.svg" color="#5bbad5" />
      <link rel="shortcut icon" href="/favicon.ico" />
      <meta name="msapplication-TileColor" content="#da532c" />
      <meta name="msapplication-config" content="/browserconfig.xml" />
      <meta name="theme-color" content="#ffffff" />

      <meta property="og:url" content="https://craft.watch/" />
      <meta property="og:title" content={props.title} />
      <meta property="og:description" content={props.description} />
      <meta property="og:image" content="https://craft.watch/craft-watch.jpg" />
      <meta property="og:type" content="website" />
    </Head>

    {props.children}
  </>
);

export default Page;