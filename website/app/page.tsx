'use client';

import * as React from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { 
  CloudRain, 
  MapPin, 
  Zap, 
  Smartphone, 
  BarChart3, 
  Bell, 
  Globe, 
  ShieldCheck,
  Download,
  Github
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { WeatherDemo } from '@/components/weather-demo';

export default function LandingPage() {
  const screenshots = ['1.png', '2.png', '3.png', '4.png'];
  
  const features = [
    {
      icon: <MapPin className="h-6 w-6 text-primary" />,
      title: "Multiple Cities",
      description: "Manage unlimited locations worldwide with ease. Switch between them instantly."
    },
    {
      icon: <Zap className="h-6 w-6 text-primary" />,
      title: "Real-time Data",
      description: "Powered by Open-Meteo for accurate current weather and forecasts."
    },
    {
      icon: <BarChart3 className="h-6 w-6 text-primary" />,
      title: "Detailed Forecasts",
      description: "Get 7-day daily and precise hourly forecasts for any city."
    },
    {
      icon: <Smartphone className="h-6 w-6 text-primary" />,
      title: "Material YOU",
      description: "Beautiful design with dynamic colors that match your Android wallpaper."
    },
    {
      icon: <Bell className="h-6 w-6 text-primary" />,
      title: "Smart Notifications",
      description: "Customizable alerts for temperature changes and severe conditions."
    },
    {
      icon: <ShieldCheck className="h-6 w-6 text-primary" />,
      title: "Privacy First",
      description: "100% free, open-source, no ads, and no tracking. Your data stays yours."
    }
  ];

  return (
    <div className="flex flex-col min-h-screen">
      {/* Hero Section */}
      <section className="relative py-20 lg:py-32 overflow-hidden bg-background">
        <div className="container px-4 md:px-6 relative z-10">
          <div className="flex flex-col lg:flex-row items-center gap-12">
            <div className="flex-1 space-y-6 text-center lg:text-left">
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
              >
                <Badge variant="secondary" className="mb-4">
                  Now with Cloud Sync v1.5.5
                </Badge>
                <h1 className="text-4xl md:text-6xl font-extrabold tracking-tighter">
                  Weather Tracking <br />
                  <span className="text-primary">Perfectly Reimagined</span>
                </h1>
                <p className="mt-4 text-xl text-muted-foreground max-w-[600px] mx-auto lg:mx-0">
                  A modern, open-source weather application for Android. 
                  Minimalist design inspired by MeteoSwiss, powered by Material YOU.
                </p>
                <div className="flex flex-wrap justify-center lg:justify-start gap-4 mt-8">
                  <Button size="lg" asChild>
                    <Link href="#download">
                      <Download className="mr-2 h-5 w-5" />
                      Download App
                    </Link>
                  </Button>
                  <Button size="lg" variant="outline" asChild>
                    <Link href="https://github.com/FreetimeMaker/GeoWeather" target="_blank">
                      <Github className="mr-2 h-5 w-5" />
                      View on GitHub
                    </Link>
                  </Button>
                </div>
              </motion.div>
            </div>
            <div className="flex-1 relative">
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.8, delay: 0.2 }}
                className="relative z-10 mx-auto w-[280px] md:w-[320px] aspect-[9/19] rounded-[3rem] border-8 border-muted bg-muted shadow-2xl overflow-hidden"
              >
                <Image
                  src="/screenshots/1.png"
                  alt="GeoWeather App Screenshot"
                  fill
                  className="object-cover"
                  priority
                />
              </motion.div>
              {/* Decorative elements */}
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[140%] h-[140%] bg-primary/10 rounded-full blur-3xl -z-10" />
            </div>
          </div>
        </div>
      </section>

      {/* Screenshots Section */}
      <section className="py-20 bg-muted/30">
        <div className="container px-4 md:px-6 text-center mb-12">
          <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl">App Showcase</h2>
          <p className="mt-4 text-muted-foreground">Take a look at the clean and intuitive Material YOU interface.</p>
        </div>
        <div className="flex overflow-x-auto pb-10 gap-6 px-4 md:justify-center scrollbar-hide">
          {screenshots.map((img, i) => (
            <motion.div
              key={i}
              whileHover={{ y: -10 }}
              className="flex-shrink-0 w-[240px] aspect-[9/19] rounded-2xl border bg-card shadow-lg overflow-hidden"
            >
              <Image
                src={`/screenshots/${img}`}
                alt={`Screenshot ${i + 1}`}
                width={240}
                height={506}
                className="object-cover"
              />
            </motion.div>
          ))}
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 bg-background">
        <div className="container px-4 md:px-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, i) => (
              <Card key={i} className="border-none shadow-none bg-transparent">
                <CardContent className="pt-6 px-0">
                  <div className="mb-4 p-3 rounded-lg bg-primary/10 w-fit">
                    {feature.icon}
                  </div>
                  <h3 className="text-xl font-bold mb-2">{feature.title}</h3>
                  <p className="text-muted-foreground">{feature.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Live Demo Section */}
      <section id="demo" className="py-20 bg-muted/50">
        <div className="container px-4 md:px-6">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl">Try the Data</h2>
            <p className="mt-4 text-muted-foreground">Experience the same precise weather data used in our Android app.</p>
          </div>
          <WeatherDemo />
        </div>
      </section>

      {/* Download Section */}
      <section id="download" className="py-20 bg-primary text-primary-foreground">
        <div className="container px-4 md:px-6 text-center">
          <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl mb-6">Get GeoWeather Today</h2>
          <p className="text-xl mb-12 opacity-90 max-w-[700px] mx-auto text-primary-foreground/80">
            Available on multiple platforms. Choose your preferred way to stay updated with the weather.
          </p>
          <div className="flex flex-wrap justify-center items-center gap-8">
            <Link href="https://github.com/FreetimeMaker/GeoWeather/releases/latest" className="transition-transform hover:scale-105">
              <Image src="/badges/badge_github.png" alt="Get it on GitHub" width={180} height={60} />
            </Link>
            <Link href="https://f-droid.org/packages/com.freetime.geoweather" className="transition-transform hover:scale-105">
              <Image src="/badges/badge_f_droid.png" alt="Get it on F-Droid" width={180} height={60} />
            </Link>
            <Link href="https://apps.obtainium.imranr.dev/" className="transition-transform hover:scale-105">
              <Image src="/badges/badge_obtainium.png" alt="Get it on Obtainium" width={180} height={60} />
            </Link>
            <Link href="https://github-store.org/app?repo=FreetimeMaker/GeoWeather" className="transition-transform hover:scale-105">
              <Image src="/badges/badge_github_store.png" alt="Get it on GitHub Store" width={160} height={50} />
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 border-t bg-background">
        <div className="container px-4 md:px-6 flex flex-col md:flex-row justify-between items-center gap-6">
          <div className="flex items-center gap-2">
            <CloudRain className="h-6 w-6 text-primary" />
            <span className="font-bold">GeoWeather</span>
          </div>
          <p className="text-sm text-muted-foreground">
            Developed with ❤️ by FreetimeMaker. Licensed under Apache-2.0.
          </p>
          <div className="flex items-center gap-6">
            <Link href="https://github.com/FreetimeMaker/GeoWeather" className="text-muted-foreground hover:text-foreground">
              <Github className="h-5 w-5" />
            </Link>
            <Link href="#" className="text-muted-foreground hover:text-foreground text-sm font-medium">
              Privacy Policy
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
